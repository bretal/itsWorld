import { useAppStore } from '@/stores/appStore';

export const useApp = () => {
  const currentPetId = useAppStore((state) => state.currentPetId);
  const currentPet = useAppStore((state) => state.currentPet);
  const pets = useAppStore((state) => state.pets);
  const loading = useAppStore((state) => state.loading);
  const error = useAppStore((state) => state.error);

  const setCurrentPetId = useAppStore((state) => state.setCurrentPetId);
  const setCurrentPet = useAppStore((state) => state.setCurrentPet);
  const setPets = useAppStore((state) => state.setPets);
  const setLoading = useAppStore((state) => state.setLoading);
  const setError = useAppStore((state) => state.setError);
  const clearError = useAppStore((state) => state.clearError);

  return {
    currentPetId,
    currentPet,
    pets,
    loading,
    error,
    setCurrentPetId,
    setCurrentPet,
    setPets,
    setLoading,
    setError,
    clearError,
  };
};
