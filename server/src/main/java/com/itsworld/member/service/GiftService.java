package com.itsworld.member.service;

import com.itsworld.member.dto.GiftView;
import com.itsworld.member.dto.MemberGiftRequest;
import com.itsworld.member.entity.MemberGift;

import java.util.List;

/**
 * 会员礼包业务接口
 *
 * @author Claude
 * @date 2026-11-07
 */
public interface GiftService {

    /**
     * 用户可见礼包列表
     *
     * @param userId 用户ID
     * @return 礼包列表
     */
    List<GiftView> listGifts(Long userId);

    /**
     * 领取礼包
     *
     * @param userId 用户ID
     * @param giftId 礼包ID
     */
    void claimGift(Long userId, Long giftId);

    /**
     * 管理端礼包列表
     *
     * @return 礼包列表
     */
    List<MemberGift> getAdminGifts();

    /**
     * 新建礼包
     *
     * @param request 礼包配置
     * @return 礼包
     */
    MemberGift createGift(MemberGiftRequest request);

    /**
     * 更新礼包
     *
     * @param giftId  礼包ID
     * @param request 礼包配置
     * @return 礼包
     */
    MemberGift updateGift(Long giftId, MemberGiftRequest request);

    /**
     * 下架礼包
     *
     * @param giftId 礼包ID
     */
    void disableGift(Long giftId);

}
