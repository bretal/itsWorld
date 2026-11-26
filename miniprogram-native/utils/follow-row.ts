import type { FollowUser } from '../services/community';
import { resolveImage } from './image';

export interface FollowRow extends FollowUser {
  avatarSrc: string;
  displayName: string;
  initial: string;
  actionText: string;
}

export function followActionText(user: FollowUser): string {
  if (user.isFollowing) {
    return user.followsMe ? '互相关注' : '已关注';
  }
  return user.followsMe ? '回关' : '关注';
}

export async function toFollowRow(user: FollowUser): Promise<FollowRow> {
  const displayName = user.nickName || '宠友';
  return {
    ...user,
    displayName,
    initial: Array.from(displayName)[0],
    avatarSrc: (await resolveImage(user.avatarUrl)) || '',
    actionText: followActionText(user),
  };
}
