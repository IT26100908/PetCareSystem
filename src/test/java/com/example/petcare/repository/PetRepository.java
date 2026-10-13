package com.example.petcare.repository;

import com.petcare.model.Pet;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;

import java.util.List;

/** Stores pets in data/pets.txt. */
@Repository
public class PetRepository extends FileRepository<Pet> {

    public PetRepository() {
        super("pets.txt");
    }

    /** Used by unit tests to point the repository at a temporary file. */
    public PetRepository(Path path) {
        super(path);
    }

    /** id|ownerId|name|species|breed|age|medicalHistory */
    @Override
    protected Pet fromRecord(String[] f) {
        return new Pet(f[0], f[1], f[2], f[3], f[4], Integer.parseInt(f[5]), f[6]);
    }

    public List<Pet> findByOwnerId(String ownerId) {
        return findAll().stream().filter(p -> p.getOwnerId().equals(ownerId)).toList();
    }
}
