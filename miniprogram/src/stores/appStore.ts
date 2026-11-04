import { create } from 'zustand';
import Taro from '@tarojs/taro';
import { petService } from '@/services/petService';
import type { Pet } from '@/types/pet';

interface AppStore {
  currentPetId: number | null;
  currentPet: Pet | null;
  pets: Pet[];
  loading: boolean;
  error: string | null;

  // 方法
  setCurrentPetId: (id: number) => void;
  setCurrentPet: (pet: Pet) => void;
  fetchPets: () => Promise<void>;
  selectPet: (id: number) => void;
  clearAppState: () => void;
}

export const useApp = create<AppStore>((set) => ({
  currentPetId: null,
  currentPet: null,
  pets: [],
  loading: false,
  error: null,

  // 设置当前宠物 ID
  setCurrentPetId: (id: number) => {
    Taro.setStorageSync('currentPetId', id);
    set({ currentPetId: id });
  },

  // 设置当前宠物信息
  setCurrentPet: (pet: Pet) => {
    set({ currentPet: pet });
  },

  // 获取宠物列表
  fetchPets: async () => {
    set({ loading: true, error: null });
    try {
      const pets = await petService.listPets();
      set({ pets, loading: false });

      // 如果有宠物，自动选择第一只
      if (pets.length > 0) {
        const currentPetId = Taro.getStorageSync('currentPetId') || pets[0].id;
        const currentPet = pets.find((p) => p.id === currentPetId) || pets[0];
        set({ currentPetId, currentPet });
        Taro.setStorageSync('currentPetId', currentPetId);
      }
    } catch (error: any) {
      set({
        error: error.message || '获取宠物列表失败',
        loading: false,
      });
    }
  },

  // 选择宠物
  selectPet: (id: number) => {
    set((state) => {
      const pet = state.pets.find((p) => p.id === id);
      if (pet) {
        Taro.setStorageSync('currentPetId', id);
        return { currentPetId: id, currentPet: pet };
      }
      return state;
    });
  },

  // 清除应用状态
  clearAppState: () => {
    Taro.removeStorageSync('currentPetId');
    set({
      currentPetId: null,
      currentPet: null,
      pets: [],
    });
  },
}));

export default useApp;
