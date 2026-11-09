package com.pawpal.repository;

import com.pawpal.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;
import java.util.List;

@Repository
public class PetRepository extends FileRepository<Pet> {
    public static final String HEADER = "id,ownerId,name,species,breed,ageYears,gender,notes,photoFile";

    public PetRepository(@Value("${pawpal.data-dir:data}") String dataDir) {
        super(Path.of(dataDir, "pets.txt"), HEADER);
    }

    @Override protected String[] toFields(Pet p) {
        return new String[]{String.valueOf(p.getId()), String.valueOf(p.getOwnerId()), p.getName(),
                p.getSpecies().name(), p.getBreed(), String.valueOf(p.getAgeYears()), p.getGender().name(), p.getNotes(), p.getPhotoFile()};
    }

    @Override protected Pet fromFields(String[] f) {
        Pet p = PetFactory.create(Species.valueOf(f[3]));
        p.setId(Long.parseLong(f[0]));
        p.setOwnerId(Long.parseLong(f[1]));
        p.setName(f[2]);
        p.setBreed(f[4]);
        p.setAgeYears(Integer.parseInt(f[5]));
        p.setGender(Gender.valueOf(f[6]));
        p.setNotes(f[7]);
        p.setPhotoFile(f.length > 8 ? f[8] : "");      // column added later: old files have 8 columns
        return p;
    }

    public List<Pet> findByOwner(long ownerId) { return findWhere(p -> p.getOwnerId() == ownerId); }
}
