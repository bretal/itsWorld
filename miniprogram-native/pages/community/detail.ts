import { requireLogin } from '../../utils/auth';
import {
  createComment,
  getCommentsPage,
  getPost,
  likePost,
  unlikePost,
} from '../../services/community';
import type { Comment, Post } from '../../services/community';
import { relativeTime } from '../../utils/format';
import { resolveImage, resolveImages } from '../../utils/image';

interface CommentItem extends Comment {
  timeText?: string;
  displayName?: string;
  displayAvatar?: string;
  avatarSrc?: string;
}

Page({
  data: {
    id: 0,
    post: null as Post | null,
    photos: [] as string[],
    postAvatarSrc: '',
    liked: false,
    timeText: '',
    comments: [] as CommentItem[],
    commentsLoading: true,
    commentsLoadingMore: false,
    commentsPage: 1,
    commentsSize: 10,
    commentsTotal: 0,
    commentsHasMore: false,
    commentContent: '',
    sending: false,
    loading: true,
  },

  onLoad(options: any) {
    if (!requireLogin()) {
      return;
    }
    const id = Number(options?.id || 0);
    this.setData({ id });
    if (id) {
      this.loadData(id);
    }
  },

  async loadData(id: number) {
    try {
      const [post, commentPage] = await Promise.all([
        getPost(id),
        getCommentsPage(id, 1, this.data.commentsSize).catch(() => null),
      ]);
      const postAvatarSrc = await resolveImage(post.userAvatar);
      const records = commentPage?.records || [];
      this.setData({
        post,
        postAvatarSrc,
        photos: await resolveImages(post.photoUrls || []),
        liked: Boolean(post.isLiked),
        timeText: relativeTime(post.createdAt),
        comments: await this.mapComments(records),
        commentsPage: commentPage?.pageNum || 1,
        commentsTotal: commentPage?.total || 0,
        commentsHasMore: Boolean(
          commentPage && commentPage.pageNum < commentPage.totalPages,
        ),
        commentsLoading: false,
        loading: false,
      });
    } catch (error) {
      this.setData({ loading: false, commentsLoading: false });
    }
  },

  async mapComments(comments: Comment[]): Promise<CommentItem[]> {
    return Promise.all(
      comments.map(async (comment) => ({
        ...comment,
        timeText: relativeTime(comment.createdAt),
        displayName: comment.username || '宠友',
        displayAvatar:
          comment.userAvatar || '/assets/avatar-placeholder.png',
        avatarSrc: await resolveImage(comment.userAvatar),
      })),
    );
  },

  async loadComments(postId: number, reset: boolean) {
    const page = reset ? 1 : this.data.commentsPage + 1;
    if (reset) {
      this.setData({ commentsLoading: true });
    } else {
      this.setData({ commentsLoadingMore: true });
    }

    try {
      const result = await getCommentsPage(postId, page, this.data.commentsSize);
      const items = await this.mapComments(result.records || []);
      this.setData({
        comments: reset ? items : this.data.comments.concat(items),
        commentsPage: result.pageNum,
        commentsTotal: result.total,
        commentsHasMore: result.pageNum < result.totalPages,
        commentsLoading: false,
        commentsLoadingMore: false,
      });
    } catch (error) {
      this.setData({ commentsLoading: false, commentsLoadingMore: false });
    }
  },

  loadMoreComments() {
    if (
      !this.data.post ||
      !this.data.commentsHasMore ||
      this.data.commentsLoadingMore
    ) {
      return;
    }
    this.loadComments(this.data.post.id, false);
  },

  async toggleLike() {
    const { post, liked } = this.data;
    if (!post) {
      return;
    }

    const nextLiked = !liked;
    this.setData({
      liked: nextLiked,
      post: {
        ...post,
        likeCount: Math.max((post.likeCount || 0) + (nextLiked ? 1 : -1), 0),
      },
    });

    try {
      if (nextLiked) {
        await likePost(post.id);
      } else {
        await unlikePost(post.id);
      }
    } catch (error) {
      this.setData({ post, liked });
    }
  },

  onCommentInput(event: any) {
    this.setData({ commentContent: event.detail.value });
  },

  async sendComment() {
    const { post, commentContent, sending } = this.data;
    if (!post || sending) {
      return;
    }
    if (!commentContent.trim()) {
      wx.showToast({ title: '说点什么吧', icon: 'none' });
      return;
    }

    this.setData({ sending: true });
    try {
      await createComment(post.id, commentContent.trim());
      this.setData({
        commentContent: '',
        post: {
          ...post,
          commentCount: (post.commentCount || 0) + 1,
        },
      });
      await this.loadComments(post.id, true);
      wx.showToast({ title: '评论成功', icon: 'success' });
    } catch (error) {
      // request 已统一提示
    } finally {
      this.setData({ sending: false });
    }
  },
});
