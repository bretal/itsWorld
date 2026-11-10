package com.itsworld.member.service;

import com.itsworld.member.dto.MemberCenterRequests.*;
import com.itsworld.member.mapper.MemberAccountMapper;
import com.itsworld.member.mapper.MemberCenterMapper;
import com.itsworld.member.entity.MemberAccount;
import com.itsworld.commerce.order.entity.*;
import com.itsworld.commerce.order.mapper.*;
import com.itsworld.commerce.order.event.CommerceOrderStatusChangedEvent;
import com.itsworld.common.exception.BusinessException;
import com.itsworld.common.result.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

/** 付费会员与成长值礼品兑换。与现有订单支付事务共用数据源。 */
@Service
@RequiredArgsConstructor
public class MemberCenterService {
    private final MemberCenterMapper memberCenterMapper;
    private final MemberService members;
    private final MemberAccountMapper accounts;
    private final CommerceOrderMapper orders;
    private final CommerceOrderItemMapper items;

    public List<Map<String,Object>> plans(boolean admin) {
        return memberCenterMapper.selectPlans(!admin);
    }

    @Transactional
    public void savePlan(Long id, Plan p) {
        long count=memberCenterMapper.countActiveLevel(p.levelId());
        require(count == 1,"权益档位不存在或已停用");
        if(id == null) {
            memberCenterMapper.insertPlan(p.name(),p.levelId(),p.price(),p.durationDays(),p.status());
        } else {
            require(memberCenterMapper.updatePlan(id,p.name(),p.levelId(),p.price(),p.durationDays(),p.status())==1,"方案不存在");
        }
    }

    @Transactional
    public String purchase(Long userId, Purchase request) {
        members.getOrCreateAccount(userId);
        MemberAccount account=accounts.selectByUserIdForUpdate(userId);
        String key="MEMBER:"+request.requestKey();
        CommerceOrder previous=orders.selectByUserAndIdempotency(userId,key);
        if(previous != null) {
            Long target=memberCenterMapper.selectOrderItemTargetId(previous.getId());
            require(Objects.equals(target,request.planId()),"幂等键已用于其他会员方案");
            return previous.getOrderNo();
        }
        List<Map<String,Object>> rows=memberCenterMapper.selectPurchasablePlan(request.planId());
        require(!rows.isEmpty(),"会员方案不可购买");
        Map<String,Object> plan=rows.get(0);
        long level=((Number)plan.get("level_id")).longValue();
        require(!account.isActive() || Objects.equals(account.getLevelId(),level),"有效期内暂不支持更换会员档位");
        long price=((Number)plan.get("price")).longValue();
        int days=((Number)plan.get("duration_days")).intValue();
        LocalDateTime now=LocalDateTime.now();
        CommerceOrder order=CommerceOrder.builder().orderNo("MEM"+UUID.randomUUID().toString().replace("-",""))
                .userId(userId).orderType("MEMBERSHIP").orderStatus("WAIT_PAY").paymentStatus("UNPAID")
                .fulfillmentStatus("PENDING").originalAmount(price).discountAmount(0L).payableAmount(price)
                .paidAmount(0L).refundedAmount(0L).idempotencyKey(key).expireTime(now.plusMinutes(15))
                .createdAt(now).updatedAt(now).build();
        orders.insert(order);
        items.insert(CommerceOrderItem.builder().orderId(order.getId()).itemType("MEMBERSHIP")
                .targetId(request.planId()).quantity(1).titleSnapshot((String)plan.get("name"))
                .specSnapshot("{\"levelId\":"+level+",\"durationDays\":"+days+"}")
                .unitPrice(price).originalTotal(price).discountAllocated(0L).payableTotal(price)
                .fulfilledQty(0).refundedAmount(0L).createdAt(now).build());
        return order.getOrderNo();
    }

    @EventListener
    @Transactional
    public void onPaid(CommerceOrderStatusChangedEvent event) {
        CommerceOrder order=event.getOrder();
        if(!"MEMBERSHIP".equals(order.getOrderType()) || !"PAID".equals(event.getAction())) { return; }
        MemberAccount account=accounts.selectByUserIdForUpdate(order.getUserId());
        long granted=memberCenterMapper.countGrantByOrder(order.getId());
        if(granted > 0) { return; }
        Map<String,Object> spec=memberCenterMapper.selectGrantSpec(order.getId());
        long level=Long.parseLong(spec.get("levelId").toString());
        int days=Integer.parseInt(spec.get("days").toString());
        require(!account.isActive() || Objects.equals(account.getLevelId(),level),"会员档位发生变化，请取消订单后重新购买");
        LocalDateTime now=LocalDateTime.now();
        LocalDateTime start=account.isActive() ? account.getMemberExpiresAt() : now;
        LocalDateTime end=start.plusDays(days);
        memberCenterMapper.insertGrant(order.getId(),order.getUserId(),level,start,end);
        account.setLevelId(level);
        account.setMemberExpiresAt(end);
        account.setUpdatedAt(now);
        accounts.updateById(account);
        orders.completeIfPaid(order.getId());
        memberCenterMapper.markOrderItemFulfilled(order.getId());
    }

    public List<Map<String,Object>> rewards(boolean admin) {
        return memberCenterMapper.selectRewards(!admin);
    }

    @Transactional
    public void saveReward(Long id, Reward p) {
        if(id == null) {
            memberCenterMapper.insertReward(p.name(),p.growthCost(),p.stock(),p.status());
        } else {
            require(memberCenterMapper.updateReward(id,p.name(),p.growthCost(),p.stock(),p.status())==1,"礼品不存在");
        }
    }

    @Transactional
    public String redeem(Long userId, Redeem request) {
        members.getOrCreateAccount(userId);
        accounts.selectByUserIdForUpdate(userId);
        List<Map<String,Object>> existing=memberCenterMapper.selectRedemptionByRequest(userId,request.requestKey());
        if(!existing.isEmpty()) {
            require(((Number)existing.get(0).get("product_id")).longValue()==request.productId(),"幂等键已用于其他礼品");
            return (String)existing.get(0).get("redemption_no");
        }
        List<Map<String,Object>> products=memberCenterMapper.selectRewardForUpdate(request.productId());
        require(!products.isEmpty(),"礼品不可兑换");
        Map<String,Object> p=products.get(0);
        int cost=((Number)p.get("growth_cost")).intValue();
        require(memberCenterMapper.decrementRewardStock(request.productId())==1,"礼品库存不足");
        String no="REW"+UUID.randomUUID().toString().replace("-","");
        members.award(userId,-cost,"REDEMPTION",no,"兑换礼品："+p.get("name"));
        memberCenterMapper.insertRedemption(no,userId,request.productId(),(String)p.get("name"),cost,request.requestKey());
        return no;
    }

    public List<Map<String,Object>> redemptions(Long userId,int page,int size) {
        int limit=Math.min(Math.max(size,1),100);
        return memberCenterMapper.selectRedemptions(userId,limit,(long)(Math.max(page,1)-1)*limit);
    }

    @Transactional
    public void fulfill(String no,Long adminId) {
        int changed=memberCenterMapper.fulfillRedemption(no,adminId);
        if(changed==0) {
            long count=memberCenterMapper.countFulfilled(no);
            require(count==1,"兑换记录不存在或状态不可核销");
        }
    }

    private void require(boolean valid,String message) {
        if(!valid) { throw new BusinessException(ErrorCode.BUSINESS_ERROR.getCode(),message); }
    }
}
