import { requireLogin } from '../../../utils/auth';

Page({
  data: { productId: '' },
  onLoad(options: { id?: string }) {
    if (requireLogin() && /^[1-9][0-9]*$/.test(options.id || '')) {
      this.setData({ productId: options.id });
    }
  },
});
