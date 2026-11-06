/**
 * 最小化的微信小程序全局类型声明。
 * 目的：不依赖 npm 包时也能用 tsc 做基础类型检查；
 * 微信开发者工具本身会提供 wx/Page/Component 的运行时与编译支持。
 */
declare const wx: any;

declare function App(options: any): void;
declare function Page(options: any): void;
declare function Component(options: any): void;
declare function getApp<T = any>(): T;
declare function getCurrentPages(): any[];

interface IAppOption {
  globalData: {
    user: any;
    currentPetId: number | null;
  };
}
