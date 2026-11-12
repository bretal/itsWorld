package com.itsworld.assistant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itsworld.assistant.entity.*;
import com.itsworld.assistant.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import static com.itsworld.assistant.service.AssistantErrors.error;

@Service
@RequiredArgsConstructor
public class AssistantTurnService {
    private final AssistantSessionMapper sessions;
    private final AssistantTurnMapper turns;
    private final AssistantQuotaMapper quotas;

    public AssistantSession owned(Long user,Long id) {
        var session=sessions.selectById(id);
        if (session==null || !user.equals(session.getUserId())) throw error("ASSISTANT_NOT_FOUND");
        return session;
    }
    @Transactional
    public AssistantSession create(Long user) {
        quotas.ensure(user); quotas.lock(user);
        LocalDateTime now=LocalDateTime.now();
        if (sessions.selectCount(new LambdaQueryWrapper<AssistantSession>().eq(AssistantSession::getUserId,user)
                .ge(AssistantSession::getCreatedAt,now.toLocalDate().atStartOfDay()))>=30) throw error("ASSISTANT_RATE_LIMITED");
        var session=new AssistantSession(); session.setUserId(user); session.setTitle("新对话");
        session.setCreatedAt(now); session.setUpdatedAt(now); sessions.insert(session); return session;
    }
    @Transactional
    public AssistantTurn acquire(Long user,Long sessionId,String request,String question,String model) {
        quotas.ensure(user); var quota=quotas.lock(user); owned(user,sessionId);
        LocalDateTime now=LocalDateTime.now();
        var turn=turns.selectOne(new LambdaQueryWrapper<AssistantTurn>().eq(AssistantTurn::getUserId,user).eq(AssistantTurn::getRequestId,request));
        String hash=hash(question);
        if (turn!=null) {
            if (!sessionId.equals(turn.getSessionId()) || !hash.equals(turn.getContentHash())) throw error("ASSISTANT_REQUEST_CONFLICT");
            if ("SUCCEEDED".equals(turn.getStatus())) return turn;
            if ("PENDING".equals(turn.getStatus()) && turn.getLeaseUntil().isAfter(now)) throw error("ASSISTANT_BUSY");
        }
        if (quota.getLeaseUntil()!=null && quota.getLeaseUntil().isAfter(now)) throw error("ASSISTANT_BUSY");
        LocalDateTime hour=now.truncatedTo(ChronoUnit.HOURS);
        if (!hour.equals(quota.getHourStart())) { quota.setHourStart(hour); quota.setAttempts(0); }
        if (quota.getAttempts()>=20) throw error("ASSISTANT_RATE_LIMITED");
        String token=UUID.randomUUID().toString();
        quota.setAttempts(quota.getAttempts()+1); quota.setActiveRequestId(request);
        quota.setLeaseToken(token); quota.setLeaseUntil(now.plusSeconds(120)); quota.setUpdatedAt(now); quotas.updateById(quota);
        if (turn==null) {
            turn=new AssistantTurn(); turn.setUserId(user); turn.setSessionId(sessionId); turn.setRequestId(request);
            turn.setContentHash(hash); turn.setQuestion(question); turn.setAttemptCount(0); turn.setCreatedAt(now);
        }
        turn.setStatus("PENDING"); turn.setAttemptCount(turn.getAttemptCount()+1); turn.setLeaseToken(token);
        turn.setLeaseUntil(now.plusSeconds(120)); turn.setModel(model); turn.setUpdatedAt(now);
        if (turn.getId()==null) turns.insert(turn); else turns.updateById(turn);
        return turn;
    }
    @Transactional
    public void finish(AssistantTurn execution,String answer,String products,int prompt,int completion) {
        var quota=quotas.lock(execution.getUserId()); var turn=turns.selectById(execution.getId());
        if (!owns(quota,turn,execution)) throw error("ASSISTANT_BUSY");
        LocalDateTime now=LocalDateTime.now();
        turn.setStatus("SUCCEEDED"); turn.setAnswer(answer); turn.setProductsJson(products);
        turn.setPromptTokens(prompt); turn.setCompletionTokens(completion); turn.setUpdatedAt(now); turns.updateById(turn);
        var session=owned(execution.getUserId(),execution.getSessionId());
        if ("新对话".equals(session.getTitle())) session.setTitle(execution.getQuestion().substring(0,Math.min(30,execution.getQuestion().length())));
        session.setUpdatedAt(now); sessions.updateById(session); release(quota);
    }
    @Transactional
    public void fail(AssistantTurn execution,String category) {
        var quota=quotas.lock(execution.getUserId()); var turn=turns.selectById(execution.getId());
        if (!owns(quota,turn,execution)) return;
        turn.setStatus("FAILED"); turn.setErrorCode(category); turn.setUpdatedAt(LocalDateTime.now()); turns.updateById(turn); release(quota);
    }
    private boolean owns(AssistantQuota q,AssistantTurn t,AssistantTurn execution) {
        return q!=null && t!=null && "PENDING".equals(t.getStatus())
                && t.getLeaseUntil()!=null && t.getLeaseUntil().isAfter(LocalDateTime.now())
                && execution.getLeaseToken().equals(t.getLeaseToken()) && execution.getLeaseToken().equals(q.getLeaseToken());
    }
    private void release(AssistantQuota q) {
        // Explicit SQL nulls: MyBatis-Plus ignores null entity fields by default.
        quotas.update(null,new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<AssistantQuota>()
                .eq(AssistantQuota::getUserId,q.getUserId()).eq(AssistantQuota::getLeaseToken,q.getLeaseToken())
                .set(AssistantQuota::getActiveRequestId,null).set(AssistantQuota::getLeaseToken,null)
                .set(AssistantQuota::getLeaseUntil,null).set(AssistantQuota::getUpdatedAt,LocalDateTime.now()));
    }
    private String hash(String text) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
