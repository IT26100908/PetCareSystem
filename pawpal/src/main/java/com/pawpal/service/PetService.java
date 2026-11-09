package com.pawpal.service;

import com.pawpal.exception.BusinessRuleException;
import com.pawpal.exception.NotFoundException;
import com.pawpal.exception.ValidationException;
import com.pawpal.form.PetForm;
import com.pawpal.model.*;
import com.pawpal.repository.*;
import com.pawpal.storage.ImagePhotoStorage;
import com.pawpal.storage.PetPhotoStorage;
import com.pawpal.util.Check;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PetService {
    private final PetRepository pets;
    private final UserService userService;
    private final AppointmentRepository appointments;
    private final MedicalRecordRepository records;
    private final VaccinationRepository vaccinations;
    private final PetPhotoStorage photos;

    public PetService(PetRepository pets, UserService userService, AppointmentRepository appointments,
                      MedicalRecordRepository records, VaccinationRepository vaccinations, PetPhotoStorage photos) {
        this.pets = pets;
        this.userService = userService;
        this.appointments = appointments;
        this.records = records;
        this.vaccinations = vaccinations;
        this.photos = photos;
    }

    // ---------- agreed cross-module API ----------
    public Optional<Pet> findById(long id) { return pets.findById(id); }
    public boolean exists(long id) { return pets.findById(id).isPresent(); }
    public long countAll() { return pets.count(); }

    /** Pets of one owner; the caller must be that owner or an admin. */
    public List<Pet> findByOwner(long ownerId) {
        if (!userService.currentUser().canAccessOwner(ownerId)) throw new AccessDeniedException("Not your pets.");
        return pets.findByOwner(ownerId).stream().sorted(Comparator.comparing(p -> p.getName().toLowerCase())).toList();
    }

    // ---------- visibility ----------
    public List<Pet> findAllVisible() {
        User me = userService.currentUser();
        return pets.findAll().stream().filter(p -> me.canAccessOwner(p.getOwnerId()))
                .sorted(Comparator.comparingLong(Pet::getId)).toList();
    }
    public Set<Long> visibleIds() {
        return findAllVisible().stream().map(Pet::getId).collect(Collectors.toSet());
    }
    public long countVisible() { return findAllVisible().size(); }

    /** Same filters plus a sort: name (default) | age | oldest | species | newest. */
    public List<Pet> search(String q, Species species, String sort) {
        List<Pet> list = new java.util.ArrayList<>(search(q, species));
        switch (sort == null ? "" : sort) {
            case "age" -> list.sort(java.util.Comparator.comparingInt(Pet::getAgeYears).thenComparing(p -> p.getName().toLowerCase()));
            case "oldest" -> list.sort(java.util.Comparator.comparingInt(Pet::getAgeYears).reversed().thenComparing(p -> p.getName().toLowerCase()));
            case "species" -> list.sort(java.util.Comparator.comparing((Pet p) -> p.getSpecies().ordinal()).thenComparing(p -> p.getName().toLowerCase()));
            case "newest" -> list.sort(java.util.Comparator.comparingLong(Pet::getId).reversed());
            default -> { /* search() already sorts by name */ }
        }
        return list;
    }

    public List<Pet> search(String q, Species species) {
        String s = q == null ? "" : q.trim().toLowerCase();
        return findAllVisible().stream()
                .filter(p -> species == null || p.getSpecies() == species)
                .filter(p -> s.isEmpty() || p.getName().toLowerCase().contains(s) || p.getBreed().toLowerCase().contains(s))
                .toList();
    }

    /** Load a pet the current user may see: NotFound if missing, AccessDenied if it belongs to someone else. */
    public Pet getAccessible(long id) {
        Pet p = pets.findById(id).orElseThrow(() -> new NotFoundException("Pet " + id + " not found"));
        if (!userService.currentUser().canAccessOwner(p.getOwnerId())) throw new AccessDeniedException("Not your pet.");
        return p;
    }

    // ---------- CRUD ----------
    public Pet create(PetForm f) {
        User me = userService.currentUser();
        long ownerId = resolveOwner(me, f);
        Pet p = PetFactory.create(Check.notNull("species", f.getSpecies(), "Species"));
        apply(p, f, ownerId);
        String newPhoto = ImagePhotoStorage.isPresent(f.getPhoto()) ? photos.store(f.getPhoto()) : null;
        if (newPhoto != null) p.setPhotoFile(newPhoto);
        try { return pets.save(p); }
        catch (RuntimeException e) { if (newPhoto != null) photos.delete(newPhoto); throw e; }
    }

    public Pet update(long id, PetForm f) {
        Pet existing = getAccessible(id);
        User me = userService.currentUser();
        long ownerId = me.isAdmin() ? resolveOwner(me, f) : existing.getOwnerId();
        Pet target = existing;
        if (f.getSpecies() != existing.getSpecies()) {         // species change = different subclass, same id
            target = PetFactory.create(Check.notNull("species", f.getSpecies(), "Species"));
            target.setId(existing.getId());
            target.setPhotoFile(existing.getPhotoFile());       // the photo belongs to the pet, not to the subclass
        }
        apply(target, f, ownerId);
        String oldPhoto = existing.getPhotoFile();
        String newPhoto = ImagePhotoStorage.isPresent(f.getPhoto()) ? photos.store(f.getPhoto()) : null;
        if (newPhoto != null) target.setPhotoFile(newPhoto);
        else if (f.isRemovePhoto()) target.setPhotoFile("");
        try {
            Pet saved = pets.save(target);
            if (!oldPhoto.isEmpty() && !oldPhoto.equals(saved.getPhotoFile())) photos.delete(oldPhoto);
            return saved;
        } catch (RuntimeException e) { if (newPhoto != null) photos.delete(newPhoto); throw e; }
    }

    /** The pet's stored photo (access-checked); NotFound when it has none. */
    public ImagePhotoStorage.StoredPhoto loadPhoto(long id) {
        Pet p = getAccessible(id);
        if (!p.hasPhoto()) throw new NotFoundException("Pet " + id + " has no photo");
        return photos.read(p.getPhotoFile()).orElseThrow(() -> new NotFoundException("Photo file for pet " + id + " is missing"));
    }

    public void delete(long id) {
        Pet p = getAccessible(id);
        int a = appointments.findByPet(id).size();
        int m = records.findByPet(id).size();
        int v = vaccinations.findByPet(id).size();
        if (a + m + v > 0)
            throw new BusinessRuleException("Cannot delete " + p.getName() + ": it still has " + a + " appointment(s), "
                    + m + " medical record(s) and " + v + " vaccination(s). Remove them first.");
        pets.deleteById(id);
        photos.delete(p.getPhotoFile());
    }

    // ---------- helpers ----------
    private long resolveOwner(User me, PetForm f) {
        if (!me.isAdmin()) return me.getId();
        Long owner = Check.notNull("ownerId", f.getOwnerId(), "Owner");
        if (!userService.exists(owner)) throw new ValidationException("ownerId", "Selected owner does not exist.");
        return owner;
    }

    private void apply(Pet p, PetForm f, long ownerId) {
        p.setOwnerId(ownerId);
        p.setName(Check.length("name", f.getName(), "Name", 1, 50));
        p.setBreed(Check.optional("breed", f.getBreed(), "Breed", 60));
        Check.range("ageYears", f.getAgeYears(), "Age", 0, 60);
        p.setAgeYears(f.getAgeYears());
        p.setGender(Check.notNull("gender", f.getGender(), "Gender"));
        p.setNotes(Check.optional("notes", f.getNotes(), "Notes", 500));
    }
}
