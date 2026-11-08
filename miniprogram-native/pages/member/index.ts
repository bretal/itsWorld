import { withStaticImages } from '../../utils/static-images';
import { requireLogin } from '../../utils/auth';
import { formatMoney } from '../../utils/format';
import { uploadImage } from '../../utils/upload';
import { getOrder, payOrder } from '../../services/commerce';
import { request } from '../../utils/request';
import * as api from '../../services/member';
import type { Overview, Task } from '../../services/member';


const CENTER_TASKS = ['DIARY_CREATE', 'PHOTO_UPLOAD', 'GROOMING_COMPLETE'];
const TASK_NAMES: Record<string, string> = {
  DIARY_CREATE: '记录宠物日记',
  PHOTO_UPLOAD: '上传宠物照片',
  GROOMING_COMPLETE: '完成一次美容服务',
};
const modal = (title: string, content: string): Promise<boolean> => new Promise(resolve => wx.showModal({ title, content, success: (r: any) => resolve(r.confirm), fail: () => resolve(false) }));
const key = () => `mp-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
const date = (value?: string) => (value || '').replace('T', ' ').slice(0, 16);

Page(withStaticImages({
  requestKeys: {} as Record<string, string>,
  getRequestKey(action: string) {
    if (!this.requestKeys[action]) this.requestKeys[action] = key();
    return this.requestKeys[action];
  },
  data: {
    top: 24, navHeight: 44, loading: true, failed: false, busy: false,
    overview: null as Overview | null, active: false, memberName: '尊享会员', validity: '开通会员，解锁专属权益',
    balance: '—', growth: '—', giftSummary: '查看可领好礼', tasks: [] as any[],

    benefits: [
      { icon: 'beauty-coins.png', name: '美容折扣', subtitle: '专属优惠' },
      { icon: 'shop-coupon.png', name: '商城特权', subtitle: '会员专享价' },
      { icon: 'benefit-birthday.png', name: '生日礼遇', subtitle: '专属好礼' },
      { icon: 'service-bubble.png', name: '专属客服', subtitle: '优先响应' },
    ],
    panel: '', panelTitle: '', panelLoading: false, panelError: false, rows: [] as any[], page: 1, more: false,
    pendingOrder: '', paymentChannel: 'WALLET', purchasePlanId: 0,
  },
  onLoad() {
    const info = wx.getWindowInfo ? wx.getWindowInfo() : wx.getSystemInfoSync();
    const capsule = wx.getMenuButtonBoundingClientRect();
    this.setData({ top: info.statusBarHeight || 24, navHeight: Math.max(44, (capsule.top - info.statusBarHeight) * 2 + capsule.height) });
  },
  onShow() { if (requireLogin()) this.load(); },
  onPullDownRefresh() { this.load().finally(() => wx.stopPullDownRefresh()); },
  back() { getCurrentPages().length > 1 ? wx.navigateBack() : wx.switchTab({ url: '/pages/profile/index' }); },
  async load() {
    try {
      if (this.data.pendingOrder) {
        const order = await getOrder(this.data.pendingOrder);
        if (order.paymentStatus === 'PAID' || ['CANCELLED', 'CLOSED'].includes(order.orderStatus)) {
          delete this.requestKeys[`buy:${this.data.purchasePlanId}`];
          this.setData({ pendingOrder: '', purchasePlanId: 0 });
        }
      }
      const overview = await api.getMemberOverview();
      const sections = await Promise.allSettled([api.getMemberTasks(), api.getMemberGifts()]);
      const tasks = sections[0].status === 'fulfilled' ? sections[0].value as Task[] : null;
      const gifts = sections[1].status === 'fulfilled' ? sections[1].value as api.Gift[] : null;
      this.setData({
        overview, active: overview.membershipStatus === 'ACTIVE', failed: false, loading: false,
        memberName: overview.membershipStatus === 'ACTIVE' ? overview.levelName || '尊享会员' : '尊享会员',
        validity: overview.memberExpiresAt ? `${overview.membershipStatus === 'ACTIVE' ? '有效期至' : '已于'} ${overview.memberExpiresAt.slice(0, 10)}${overview.membershipStatus === 'EXPIRED' ? ' 到期' : ''}` : '开通会员，解锁专属权益',
        balance: formatMoney(overview.wallet.availableBalance), growth: (overview.growthValue || 0).toLocaleString(),
        tasks: tasks === null ? [] : tasks
          .filter(task => CENTER_TASKS.includes(task.taskCode))
          .sort((a, b) => CENTER_TASKS.indexOf(a.taskCode) - CENTER_TASKS.indexOf(b.taskCode))
          .map(task => ({ ...task,
          taskName: TASK_NAMES[task.taskCode],
          label: task.rewardStatus === 1 ? '已领取' : task.claimable ? '领取奖励' : '去完成',
          icon: task.taskCode === 'PHOTO_UPLOAD' ? 'task-photo.png' : task.taskCode === 'GROOMING_COMPLETE' ? 'task-booking.png' : 'paw.png',
        })),
        giftSummary: gifts === null ? '点击重试加载好礼' : gifts.some(g => g.claimable) ? '你有好礼待领取 ›' : '查看可领好礼 ›',
      });
    } catch (_) { this.setData({ loading: false, failed: true, overview: null, active: false, memberName: '尊享会员', validity: '会员信息暂不可用', balance: '—', growth: '—', tasks: [] }); }
  },
  async openPanel(event: any) {
    const panel = event.currentTarget.dataset.panel;
    const titles: Record<string, string> = { plans: '开通 / 续费会员', wallet: '余额明细', recharge: '钱包充值', growth: '成长明细', gifts: '会员专属礼包', rewards: '成长值兑换好礼', redemptions: '我的兑换记录', benefits: '会员权益' };
    if (!this.data.overview) { this.load(); return; }
    this.setData({ panel, panelTitle: titles[panel], rows: [], page: 1, more: false });
    await this.loadPanel(false);
  },
  closePanel() { if (!this.data.busy) this.setData({ panel: '' }); },
  noop() {},
  async loadPanel(append = false) {
    if (this.data.panelLoading) return;
    const panel = this.data.panel;
    const page = append ? this.data.page + 1 : 1;
    this.setData({ panelLoading: true, panelError: false });
    try {
      let rows: any[] = []; let more = false;
      if (panel === 'plans') rows = (await api.getPlans()).map(p => ({ ...p, title: p.name, subtitle: `${p.duration_days} 天 · ${this.data.active ? '有效期内仅支持续费相同档位' : '支付后生效'}`, price: `¥${formatMoney(p.price)}`, action: this.data.active && this.data.overview?.levelId !== p.level_id ? '暂不可用' : '选择', disabled: this.data.active && this.data.overview?.levelId !== p.level_id }));
      if (panel === 'recharge') rows = (await api.getRechargePlans()).map(p => ({ ...p, title: p.planName, subtitle: `充值 ¥${formatMoney(p.principalAmount)}${p.bonusAmount ? `，赠 ¥${formatMoney(p.bonusAmount)}` : ''}`, action: '充值' }));
      if (panel === 'gifts') rows = (await api.getMemberGifts()).map(g => ({ ...g, title: g.giftName, subtitle: g.description || [g.growthReward ? `${g.growthReward}成长值` : '', g.couponTemplateName].filter(Boolean).join(' · '), action: g.claimed ? '已领取' : g.claimable ? '领取' : g.lockReason || '暂不可领', disabled: !g.claimable }));
      if (panel === 'rewards') rows = (await api.getRewards()).map(p => ({ ...p, title: p.name, subtitle: `${p.growth_cost} 成长值 · 剩余 ${p.stock} 件`, action: p.stock <= 0 ? '已兑完' : '兑换', disabled: p.stock <= 0 || p.growth_cost > (this.data.overview?.growthValue || 0) }));
      if (panel === 'benefits') rows = (this.data.overview?.benefits || []).map(b => ({ id: b.id, title: b.benefitName, subtitle: b.description || b.benefitValue || '具体权益以门店说明为准' }));
      if (panel === 'growth') { const p = await api.getGrowthLogs(page); rows = p.records.map(r => ({ id: r.id, title: r.remark || '成长值变动', subtitle: date(r.createdAt), price: `${r.changeValue > 0 ? '+' : ''}${r.changeValue}` })); more = page * 20 < p.total; }
      if (panel === 'wallet') { const p = await api.getWalletLogs(page); rows = p.records.map(r => ({ id: r.id, title: r.remark || r.txType, subtitle: date(r.createdAt), price: `${r.principalDelta + r.bonusDelta > 0 ? '+' : ''}¥${formatMoney(r.principalDelta + r.bonusDelta)}` })); more = page * 20 < p.total; }
      if (panel === 'redemptions') { const p = await api.getRedemptions(page); rows = p.map(r => ({ id: r.id, title: r.name_snapshot, subtitle: `${date(r.created_at)} · ${r.status === 'FULFILLED' ? '已领取' : '待门店交付'}`, price: `-${r.growth_cost}`, no: r.redemption_no })); more = p.length === 20; }
      if (panel === this.data.panel) this.setData({ rows: append ? this.data.rows.concat(rows) : rows, page, more });
    } catch (_) { if (panel === this.data.panel) this.setData({ panelError: true }); }
    finally { this.setData({ panelLoading: false }); }
  },
  moreRows() { this.loadPanel(true); },
  retryPanel() { this.loadPanel(false); },
  async actRow(event: any) {
    if (this.data.busy) return;
    const row = this.data.rows.find((r: any) => r.id === Number(event.currentTarget.dataset.id));
    if (!row || row.disabled) return;
    const panel = this.data.panel;
    if (panel === 'plans') {
      if (await modal('确认会员方案', `${row.title}，${row.duration_days}天，${row.price}。支付后生效，会员到期不影响成长值。`)) {
        this.setData({ busy: true });
        try { const no = await api.buyMembership(row.id, this.getRequestKey(`buy:${row.id}`)); this.setData({ pendingOrder: no, purchasePlanId: row.id }); }
        catch (_) {} finally { this.setData({ busy: false }); }
      }
      return;
    }
    if (panel === 'recharge' && !this.data.overview?.wallet.mockPayEnabled) {
      wx.showModal({ title: '线上充值尚未开放', content: '当前尚未接入微信充值，请联系门店办理线下充值。', showCancel: false }); return;
    }
    if (!['gifts', 'rewards', 'recharge'].includes(panel)) return;
    if (!(await modal('确认操作', panel === 'rewards' ? `使用 ${row.growth_cost} 成长值兑换「${row.title}」？兑换后凭记录到门店领取。` : panel === 'recharge' ? '这是开发环境模拟充值，不会发生真实微信扣款。是否继续？' : `领取「${row.title}」？`))) return;
    this.setData({ busy: true });
    try {
      if (panel === 'gifts') await api.claimGift(row.id);
      if (panel === 'rewards') { const no = await api.redeemReward(row.id, this.getRequestKey(`redeem:${row.id}`)); delete this.requestKeys[`redeem:${row.id}`]; await modal('兑换成功', `兑换号：${no}。请在“兑换记录”中查看，联系门店领取。`); }
      if (panel === 'recharge') { const order = await api.createRecharge(row.id); await api.mockRecharge(order.rechargeNo); }
      wx.showToast({ title: panel === 'rewards' ? '兑换成功' : '操作成功', icon: 'success' });
      await this.load(); await this.loadPanel(false);
    } catch (_) {} finally { this.setData({ busy: false }); }
  },
  selectChannel(event: any) { this.setData({ paymentChannel: event.currentTarget.dataset.channel }); },
  async payMembership() {
    if (this.data.busy || !this.data.pendingOrder) return;
    if (this.data.paymentChannel === 'MOCK_WECHAT' && !(await modal('开发环境模拟支付', '不会发生真实微信扣款，仅用于验证会员开通流程。是否继续？'))) return;
    this.setData({ busy: true });
    try {
      await payOrder(this.data.pendingOrder, this.data.paymentChannel);
      delete this.requestKeys[`buy:${this.data.purchasePlanId}`];
      this.setData({ pendingOrder: '', panel: '' }); await this.load();
      wx.showToast({ title: '会员已生效', icon: 'success' });
    } catch (_) {} finally { this.setData({ busy: false }); }
  },
  viewPendingOrder() { wx.navigateTo({ url: `/pages/commerce/order/detail?orderNo=${this.data.pendingOrder}` }); },
  async taskAction(event: any) {
    if (this.data.busy) return;
    const task = this.data.tasks.find((t: Task) => t.id === Number(event.currentTarget.dataset.id));
    if (!task || task.rewardStatus === 1) return;
    if (task.claimable) {
      this.setData({ busy: true }); try { await api.claimTask(task.id); await this.load(); wx.showToast({ title: '成长值已到账' }); } catch (_) {} finally { this.setData({ busy: false }); } return;
    }
    if (task.taskCode === 'PHOTO_UPLOAD') {
      wx.chooseMedia({ count: 1, mediaType: ['image'], success: async (r: any) => { this.setData({ busy: true }); try { await uploadImage(r.tempFiles[0].tempFilePath, 'pet'); await this.load(); } catch (_) {} finally { this.setData({ busy: false }); } } }); return;
    }
    if (task.taskCode === 'DAILY_CHECKIN') { this.setData({ busy: true }); try { await request({ url: '/member/tasks/check-in', method: 'POST' }); await this.load(); } catch (_) {} finally { this.setData({ busy: false }); } return; }
    const routes: Record<string, string> = { DIARY_CREATE: '/pages/diary/edit', GROOMING_COMPLETE: '/pages/commerce/grooming/index', PET_CREATE: '/pages/pet/edit', POST_CREATE: '/pages/community/create' };
    if (routes[task.taskCode]) wx.navigateTo({ url: routes[task.taskCode] });
    else wx.showModal({ title: task.taskName, content: task.description || '请按任务说明完成操作后返回领取成长值。', showCancel: false });
  },
  balanceInfo() { wx.showModal({ title: '可用余额', content: '展示可用于支付的本金和赠送余额，已扣除冻结金额。充值不会自动开通会员，会员费用需要单独支付。', showCancel: false }); },
}, ["member/beauty-coins.png", "member/benefit-beauty.png", "member/benefit-birthday.png", "member/benefit-service.png", "member/benefit-shop.png", "member/coin.png", "member/crown.png", "member/gift.png", "member/hero.jpg", "member/membership-paw.png", "member/paw.png", "member/service-bubble.png", "member/shop-coupon.png", "member/task-booking.png", "member/task-photo.png", "member/wallet-pink.png", "member/wallet.png"]));
