import { withStaticImages } from '../../utils/static-images';
import { getUser, requireLogin } from '../../utils/auth';
import { confirmUnfollow, getPostPage, likePost, setUserFollowing, unlikePost } from '../../services/community';
import type { CommunityTab, Post } from '../../services/community';
import { relativeTime } from '../../utils/format';
import { normalizeMinioUrl } from '../../utils/image';

interface PostItem extends Post {
  timeText?: string;
  photos: string[];
  liked: boolean;
  avatarSrc?: string;
}

Page(withStaticImages({
  data: {
    tabs: [
      { key: 'follow', label: '关注' },
      { key: 'recommend', label: '推荐' },
    ],
    activeTab: 'recommend',
    posts: [] as PostItem[],
    loading: true,
    loadingMore: false,
    page: 1,
    size: 10,
    total: 0,
    hasMore: true,
    emptyText: '还没有帖子，来发第一条吧',
    currentUserId: 0,
    pendingUserId: 0,
    loadFailed: '' as '' | 'refresh' | 'more',
  },

  feedRequest: 0,

  onShow() {
    const tabBar = (this as any).getTabBar?.();
    if (tabBar) {
      tabBar.setData({ selected: 2 });
    }
    if (!requireLogin()) {
      return;
    }
    this.setData({ currentUserId: getUser()?.id || 0 });
    this.loadData(this.data.activeTab, true);
  },

  onPullDownRefresh() {
    this.loadData(this.data.activeTab, true).finally(() =>
      wx.stopPullDownRefresh(),
    );
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading && !this.data.loadingMore && !this.data.pendingUserId) {
      this.loadData(this.data.activeTab, false);
    }
  },

  async loadData(tab: string, reset: boolean) {
    const request = ++this.feedRequest;
    const page = reset ? 1 : this.data.page + 1;
    this.setData({ loading: reset, loadingMore: !reset, loadFailed: '' });

    try {
      const type: CommunityTab = tab === 'follow' ? 'following' : 'recommend';
      const result = await getPostPage(type, page, this.data.size);
      if (request !== this.feedRequest) {
        return;
      }
      const items = this.mapPosts(result.records || []);
      this.setData({
        posts: reset ? items : this.data.posts.concat(items),
        page: result.pageNum,
        total: result.total,
        hasMore: result.pageNum < result.totalPages,
        emptyText:
          tab === 'follow'
            ? '暂无关注动态，去推荐里看看吧'
            : '还没有帖子，来发第一条吧',
        loading: false,
        loadingMore: false,
      });
    } catch {
      if (request === this.feedRequest) {
        this.setData({ loading: false, loadingMore: false, loadFailed: reset ? 'refresh' : 'more' });
      }
    }
  },

  retryFeed() {
    this.loadData(this.data.activeTab, this.data.loadFailed !== 'more');
  },

  async toggleFollow(event: { currentTarget: { dataset: { id: number } } }) {
    const post = this.data.posts.find((item: PostItem) => item.id === Number(event.currentTarget.dataset.id));
    if (!post || post.userId === this.data.currentUserId || this.data.pendingUserId) {
      return;
    }
    this.setData({ pendingUserId: post.userId });
    try {
      if (post.isFollowing && !(await confirmUnfollow(post.username || '这位宠友'))) {
        return;
      }
      const following = !post.isFollowing;
      await setUserFollowing(post.userId, following);
      const posts: PostItem[] = this.data.posts;
      const { activeTab, loading, loadingMore } = this.data;
      const remove = activeTab === 'follow' && !following;
      this.setData({
        posts: remove
          ? posts.filter((item) => item.userId !== post.userId)
          : posts.map((item) => item.userId === post.userId ? { ...item, isFollowing: following } : item),
      });
      wx.showToast({ title: following ? '已关注' : '已取消关注', icon: 'success' });
      if (activeTab === 'follow' || loading || loadingMore) {
        await this.loadData(activeTab, true);
      }
    } catch {
      // 连接中断时服务端可能已完成操作，重新读取关系状态。
      await this.loadData(this.data.activeTab, true);
    } finally {
      this.setData({ pendingUserId: 0 });
    }
  },

  mapPosts(list: Post[]): PostItem[] {
    return list.map((post) => ({
      ...post,
      timeText: relativeTime(post.createdAt),
      photos: (post.photoUrls || []).map(normalizeMinioUrl).filter(Boolean),
      liked: Boolean(post.isLiked),
      avatarSrc: normalizeMinioUrl(post.userAvatar),
    }));
  },

  onAvatarError(event: { currentTarget: { dataset: { id: number } } }) {
    const index = this.data.posts.findIndex((item: PostItem) =>
      item.id === Number(event.currentTarget.dataset.id) && item.avatarSrc,
    );
    if (index !== -1) {
      this.setData({ [`posts[${index}].avatarSrc`]: '' });
    }
  },

  switchTab(event: any) {
    const key = event.currentTarget.dataset.key;
    if (key === this.data.activeTab) {
      return;
    }
    this.setData({ activeTab: key, posts: [], hasMore: true });
    this.loadData(key, true);
  },

  async like(event: any) {
    const index = Number(event.currentTarget.dataset.index);
    const post = this.data.posts[index];
    if (!post) {
      return;
    }

    const nextLiked = !post.liked;
    const posts = this.data.posts.slice();
    posts[index] = {
      ...post,
      liked: nextLiked,
      likeCount: Math.max((post.likeCount || 0) + (nextLiked ? 1 : -1), 0),
    };
    this.setData({ posts });

    try {
      if (nextLiked) {
        await likePost(post.id);
      } else {
        await unlikePost(post.id);
      }
    } catch (error) {
      // 失败时回滚
      this.setData({
        posts: this.data.posts.map((item: PostItem) => item.id === post.id && item.liked === nextLiked
          ? { ...item, liked: post.liked, likeCount: post.likeCount }
          : item),
      });
    }
  },

  goPostDetail(event: any) {
    const id = Number(event.currentTarget.dataset.id);
    if (!id) {
      return;
    }
    wx.navigateTo({ url: `/pages/community/detail?id=${id}` });
  },

  goCreatePost() {
    wx.navigateTo({ url: '/pages/community/create' });
  },

  goSearchUsers() {
    wx.navigateTo({ url: '/pages/community/search' });
  },

  stopCardTap() {},

  onShareAppMessage(event: any) {
    const id = Number(event.target?.dataset?.id);
    const post = this.data.posts.find((item: PostItem) => item.id === id);
    return {
      title: post?.title || '和同频的宠友分享养宠日常',
      path: post ? `/pages/community/detail?id=${post.id}` : '/pages/community/index',
    };
  },

  goRecommend() {
    this.setData({ activeTab: 'recommend', posts: [], hasMore: true });
    this.loadData('recommend', true);
  },
}, ["avatar-placeholder.png", "community/comment.png", "community/like.png"]));
