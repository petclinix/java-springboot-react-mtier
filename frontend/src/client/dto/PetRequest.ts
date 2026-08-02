export type PetRequest = {
    name: string;
    species: string;
    gender: string;
    breed?: string | null;
    birthDate?: string | null;
    picture?: string | null;
    pictureContentType?: string | null;
};
