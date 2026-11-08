import { assetUrl } from './assets';
import { resolveImage } from './image';

/** Static source assets live in MinIO, downloaded to wx temporary paths for rendering. */
export const staticImageUrl = (key: string): string => assetUrl(`miniapp-v1/${key}`);

export function withStaticImages(options: any, keys: string[]): any {
  const onLoad = options.onLoad;
  return {
    ...options,
    data: { ...options.data, staticAssets: Object.fromEntries(keys.map(key => [key, ''])) },
    onLoad(this: any, ...args: any[]) {
      this.__staticImagesAlive = true;
      Promise.all(keys.map(async key => [key, await resolveImage(staticImageUrl(key))]))
        .then(entries => {
          if (this.__staticImagesAlive) this.setData({ staticAssets: Object.fromEntries(entries) });
        });
      return onLoad?.apply(this, args);
    },
    onUnload(this: any, ...args: any[]) {
      this.__staticImagesAlive = false;
      return options.onUnload?.apply(this, args);
    },
  };
}
