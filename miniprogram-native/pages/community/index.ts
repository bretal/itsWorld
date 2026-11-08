import { withStaticImages } from '../../utils/static-images';
import { requireLogin } from '../../utils/auth';
import { getPostPage, likePost, unlikePost } from '../../services/community';
import type { CommunityTab, Post } from '../../services/community';
import { relativeTime } from '../../utils/format';
import { resolveImage, resolveImages } from '../../utils/image';

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
  },

  onShow() {
    const tabBar = (this as any).getTabBar?.();
    if (tabBar) {
      tabBar.setData({ selected: 2 });
    }
    if (!requireLogin()) {
      return;
    }
    this.loadData(this.data.activeTab, true);
  },

  onPullDownRefresh() {
    this.loadData(this.data.activeTab, true).finally(() =>
      wx.stopPullDownRefresh(),
    );
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading && !this.data.loadingMore) {
      this.loadData(this.data.activeTab, false);
    }
  },

  async loadData(tab: string, reset: boolean) {
    const page = reset ? 1 : this.data.page + 1;
    if (reset) {
      this.setData({ loading: true });
    } else {
      this.setData({ loadingMore: true });
    }

    try {
      const type: CommunityTab = tab === 'follow' ? 'following' : 'recommend';
      const result = await getPostPage(type, page, this.data.size);
      const items = await this.mapPosts(result.records || []);
      this.setData({
        posts: reset ? items : this.data.posts.concat(items),
        page: result.pageNum,
        total: result.total,
        hasMore: result.pageNum < result.totalPages,
        emptyText:
          tab === 'follow'
            ? '还没有关注的人，去推荐里看看吧'
            : '还没有帖子，来发第一条吧',
        loading: false,
        loadingMore: false,
      });
    } catch (error) {
      this.setData({ loading: false, loadingMore: false });
    }
  },

  async mapPosts(list: Post[]): Promise<PostItem[]> {
    return Promise.all(
      (list || []).map(async (post) => ({
        ...post,
        timeText: relativeTime(post.createdAt),
        photos: await resolveImages(post.photoUrls || []),
        liked: Boolean(post.isLiked),
        avatarSrc: await resolveImage(post.userAvatar),
      })),
    );
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
      const rollback = this.data.posts.slice();
      rollback[index] = post;
      this.setData({ posts: rollback });
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
