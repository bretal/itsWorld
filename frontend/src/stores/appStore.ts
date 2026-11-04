import { create } from 'zustand';
import type { Pet } from '@/types/pet';

interface AppStore {
  currentPetId: number | null;
  currentPet: Pet | null;
  pets: Pet[];
  loading: boolean;
  error: string | null;

  // Actions
  setCurrentPetId: (petId: number | null) => void;
  setCurrentPet: (pet: Pet | null) => void;
  setPets: (pets: Pet[]) => void;
  setLoading: (loading: boolean) => void;
  setError: (error: string | null) => void;
  clearError: () => void;
}

const STORAGE_CURRENT_PET_KEY = 'currentPetId';

export const useAppStore = create<AppStore>((set) => ({
  currentPetId: localStorage.getItem(STORAGE_CURRENT_PET_KEY)
    ? Number(localStorage.getItem(STORAGE_CURRENT_PET_KEY))
    : null,
  currentPet: null,
  pets: [],
  loading: false,
  error: null,

  setCurrentPetId: (petId: number | null) => {
    if (petId) {
      localStorage.setItem(STORAGE_CURRENT_PET_KEY, petId.toString());
    } else {
      localStorage.removeItem(STORAGE_CURRENT_PET_KEY);
    }
    set({ currentPetId: petId });
  },

  setCurrentPet: (pet: Pet | null) => {
    set({ currentPet: pet });
  },

  setPets: (pets: Pet[]) => {
    set({ pets });
  },

  setLoading: (loading: boolean) => {
    set({ loading });
  },

  setError: (error: string | null) => {
    set({ error });
  },

  clearError: () => {
    set({ error: null });
  },
}));
