export function parseDate(value?: string): Date | null {
  if (!value) {
    return null;
  }
  const normalized = value.includes('T')
    ? value.replace(/-/g, '/').replace('T', ' ')
    : value.replace(/-/g, '/');
  const date = new Date(normalized);
  return Number.isNaN(date.getTime()) ? null : date;
}

export function formatAge(birthDate?: string): string {
  const birth = parseDate(birthDate);
  if (!birth) {
    return '年龄未知';
  }

  const now = new Date();
  let months =
    (now.getFullYear() - birth.getFullYear()) * 12 +
    (now.getMonth() - birth.getMonth());
  if (now.getDate() < birth.getDate()) {
    months -= 1;
  }
  months = Math.max(months, 0);

  const years = Math.floor(months / 12);
  const rest = months % 12;
  if (years > 0) {
    return rest > 0 ? `${years}岁${rest}个月` : `${years}岁`;
  }
  return `${rest}个月`;
}

export function genderText(gender?: number): string {
  if (gender === 1) {
    return '男生';
  }
  if (gender === 2) {
    return '女生';
  }
  return '未知';
}

export function formatDate(value?: string): string {
  const date = parseDate(value);
  if (!date) {
    return '';
  }
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}

export function formatDateTime(value?: string): string {
  const date = parseDate(value);
  if (!date) {
    return '';
  }
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hour = String(date.getHours()).padStart(2, '0');
  const minute = String(date.getMinutes()).padStart(2, '0');
  return `${month}月${day}日 ${hour}:${minute}`;
}

export function relativeTime(value?: string): string {
  const date = parseDate(value);
  if (!date) {
    return '';
  }

  const diff = Date.now() - date.getTime();
  const minute = 60 * 1000;
  const hour = 60 * minute;
  const day = 24 * hour;

  if (diff < minute) {
    return '刚刚';
  }
  if (diff < hour) {
    return `${Math.floor(diff / minute)}分钟前`;
  }
  if (diff < day) {
    return `${Math.floor(diff / hour)}小时前`;
  }
  if (diff < 7 * day) {
    return `${Math.floor(diff / day)}天前`;
  }
  return formatDate(value);
}

export function todayString(): string {
  const now = new Date();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${now.getFullYear()}-${month}-${day}`;
}

export function formatWeight(weight?: number | string): string {
  if (weight === undefined || weight === null || weight === '') {
    return '--';
  }
  return `${weight}kg`;
}

export function formatMoney(cents?: number | null): string {
  return ((cents || 0) / 100).toFixed(2);
}
