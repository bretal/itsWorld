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
    set((state) => ({
      currentPetId: petId,
      currentPet: petId ? state.pets.find((pet) => pet.id === petId) || null : null,
    }));
  },

  setCurrentPet: (pet: Pet | null) => {
    set({ currentPet: pet });
  },

  setPets: (pets: Pet[]) => {
    set((state) => {
      let currentPetId = state.currentPetId;
      let currentPet = pets.find((pet) => pet.id === currentPetId) || null;

      if (!currentPet && pets.length > 0) {
        currentPet = pets[0];
        currentPetId = pets[0].id;
        localStorage.setItem(STORAGE_CURRENT_PET_KEY, currentPetId.toString());
      }
      if (pets.length === 0) {
        currentPetId = null;
        currentPet = null;
        localStorage.removeItem(STORAGE_CURRENT_PET_KEY);
      }

      return { pets, currentPetId, currentPet };
    });
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
