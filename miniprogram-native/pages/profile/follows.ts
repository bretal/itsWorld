import { confirmUnfollow, getFollowStats, getFollowUsers, setUserFollowing } from '../../services/community';
import type { FollowStats, FollowTab } from '../../services/community';
import { requireLogin } from '../../utils/auth';
import { toFollowRow } from '../../utils/follow-row';
import type { FollowRow } from '../../utils/follow-row';

Page({
  data: {
    tab: 'following' as FollowTab,
    stats: null as FollowStats | null,
    rows: [] as FollowRow[],
    loading: true,
    loadingMore: false,
    loadFailed: '' as '' | 'refresh' | 'more',
    hasMore: false,
    page: 0,
    pendingUserId: 0,
  },

  listRequest: 0,
  statsRequest: 0,

  onLoad(query: { tab?: string }) {
    if (query.tab === 'followers') {
      this.setData({ tab: 'followers' });
    }
  },

  onShow() {
    if (requireLogin()) {
      this.refresh();
    }
  },

  onPullDownRefresh() {
    this.refresh().finally(() => wx.stopPullDownRefresh());
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading && !this.data.loadingMore && !this.data.pendingUserId) {
      this.loadRows(false);
    }
  },

  async refresh() {
    await Promise.all([this.loadStats(), this.loadRows(true)]);
  },

  async loadStats() {
    const request = ++this.statsRequest;
    try {
      const stats = await getFollowStats();
      if (request === this.statsRequest) {
        this.setData({ stats });
      }
    } catch {
      // 请求层已提示，后续刷新可重试。
    }
  },

  async loadRows(reset: boolean) {
    const request = ++this.listRequest;
    const { tab, page } = this.data;
    this.setData({ loading: reset, loadingMore: !reset, loadFailed: '' });
    try {
      const result = await getFollowUsers(tab, reset ? 1 : page + 1);
      const rows = await Promise.all(result.records.map(toFollowRow));
      if (request !== this.listRequest) {
        return;
      }
      this.setData({
        rows: reset ? rows : this.data.rows.concat(rows),
        page: result.pageNum,
        hasMore: result.pageNum < result.totalPages,
      });
    } catch {
      if (request === this.listRequest) {
        this.setData({ loadFailed: reset ? 'refresh' : 'more' });
      }
    } finally {
      if (request === this.listRequest) {
        this.setData({ loading: false, loadingMore: false });
      }
    }
  },

  onSwitchTab(event: { currentTarget: { dataset: { tab: FollowTab } } }) {
    const { tab } = event.currentTarget.dataset;
    if (tab !== this.data.tab) {
      this.setData({ tab, rows: [], page: 0, hasMore: false });
      this.loadRows(true);
    }
  },

  retry() {
    return this.data.loadFailed === 'more' ? this.loadRows(false) : this.refresh();
  },

  async toggleFollow(event: { currentTarget: { dataset: { id: number } } }) {
    const row = this.data.rows.find((user: FollowRow) => user.id === Number(event.currentTarget.dataset.id));
    if (!row || this.data.pendingUserId) {
      return;
    }
    this.setData({ pendingUserId: row.id });
    try {
      if (row.isFollowing && !(await confirmUnfollow(row.displayName))) {
        return;
      }
      await setUserFollowing(row.id, !row.isFollowing);
      const following = !row.isFollowing;
      const rows: FollowRow[] = this.data.rows;
      this.setData({
        rows: this.data.tab === 'following' && !following
          ? rows.filter((user) => user.id !== row.id)
          : rows.map((user) => user.id === row.id
              ? { ...user, isFollowing: following, actionText: following ? '互相关注' : '回关' }
              : user),
      });
      wx.showToast({ title: row.isFollowing ? '已取消关注' : '已关注', icon: 'success' });
      await this.refresh();
    } catch {
      // 连接中断时服务端可能已完成操作，重新读取关系状态。
      await this.refresh();
    } finally {
      this.setData({ pendingUserId: 0 });
    }
  },

  goCommunity() {
    wx.switchTab({ url: '/pages/community/index' });
  },
});
