import { confirmUnfollow, searchCommunityUsers, setUserFollowing } from '../../services/community';
import { requireLogin } from '../../utils/auth';
import { followActionText, toFollowRow } from '../../utils/follow-row';
import type { FollowRow } from '../../utils/follow-row';

Page({
  data: {
    input: '',
    keyword: '',
    rows: [] as FollowRow[],
    page: 0,
    hasMore: false,
    loading: false,
    failedPage: 0,
    pendingUserId: 0,
  },

  requestId: 0,

  onLoad() {
    requireLogin();
  },

  onInput(event: { detail: { value: string } }) {
    this.setData({ input: event.detail.value });
  },

  clearSearch() {
    ++this.requestId;
    this.setData({ input: '', keyword: '', rows: [], page: 0, hasMore: false, loading: false, failedPage: 0 });
  },

  submitSearch() {
    const keyword = this.data.input.trim();
    if (!keyword || this.data.pendingUserId) {
      return;
    }
    this.setData({ keyword, rows: [], page: 0, hasMore: false });
    return this.loadPage(1);
  },

  onReachBottom() {
    const { hasMore, loading, page, failedPage, pendingUserId } = this.data;
    if (hasMore && !loading && !failedPage && !pendingUserId) {
      return this.loadPage(page + 1);
    }
  },

  retry() {
    return this.loadPage(this.data.failedPage);
  },

  async loadPage(page: number) {
    const requestId = ++this.requestId;
    this.setData({ loading: true, failedPage: 0 });
    try {
      const result = await searchCommunityUsers(this.data.keyword, page);
      const rows = await Promise.all(result.records.map(toFollowRow));
      if (requestId === this.requestId) {
        this.setData({
          rows: page === 1 ? rows : this.data.rows.concat(rows),
          page: result.pageNum,
          hasMore: result.pageNum < result.totalPages,
        });
      }
    } catch {
      if (requestId === this.requestId) {
        this.setData({ failedPage: page });
      }
    } finally {
      if (requestId === this.requestId) {
        this.setData({ loading: false });
      }
    }
  },

  async toggleFollow(event: { currentTarget: { dataset: { id: number } } }) {
    const { rows, loading, pendingUserId } = this.data;
    const row = rows.find((item: FollowRow) => item.id === Number(event.currentTarget.dataset.id));
    if (!row || loading || pendingUserId) {
      return;
    }
    this.setData({ pendingUserId: row.id });
    try {
      if (row.isFollowing && !(await confirmUnfollow(row.displayName))) {
        return;
      }
      const isFollowing = !row.isFollowing;
      await setUserFollowing(row.id, isFollowing);
      const updated = { ...row, isFollowing };
      this.setData({
        rows: rows.map((item: FollowRow) =>
          item.id === row.id ? { ...updated, actionText: followActionText(updated) } : item),
      });
      wx.showToast({ title: isFollowing ? '已关注' : '已取消关注', icon: 'success' });
    } catch {
      await this.loadPage(1);
    } finally {
      this.setData({ pendingUserId: 0 });
    }
  },
});
