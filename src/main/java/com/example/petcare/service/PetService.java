package com.petcare.service;

import com.petcare.model.Appointment;
import com.petcare.model.Pet;
import com.petcare.repository.AppointmentRepository;
import com.petcare.repository.PetRepository;
import com.petcare.util.FileUtil;
import org.springframework.stereotype.Service;

import java.util.List;

/** CRUD rules for pets. Every operation checks that the pet belongs to the logged-in owner. */
@Service
public class PetService {

    private final PetRepository pets;
    private final AppointmentRepository appointments;

    public PetService(PetRepository pets, AppointmentRepository appointments) {
        this.pets = pets;
        this.appointments = appointments;
    }

    public Pet add(String ownerId, String name, String species, String breed, int age, String history) {
        validate(name, species, age);
        return pets.save(new Pet(FileUtil.newId(), ownerId, name.trim(), species.trim(),
                clean(breed), age, clean(history)));
    }

    public List<Pet> listByOwner(String ownerId) {
        return pets.findByOwnerId(ownerId);
    }

    public Pet getOwned(String ownerId, String petId) {
        return pets.findById(petId)
                .filter(p -> p.getOwnerId().equals(ownerId))
                .orElseThrow(() -> new IllegalArgumentException("Pet not found."));
    }

    public Pet update(String ownerId, String petId, String name, String species, String breed, int age, String history) {
        validate(name, species, age);
        Pet pet = getOwned(ownerId, petId);
        pet.setName(name.trim());
        pet.setSpecies(species.trim());
        pet.setBreed(clean(breed));
        pet.setAge(age);
        pet.setMedicalHistory(clean(history));
        return pets.save(pet);
    }

    /** Deleting a pet also cancels its future bookings so no orphan appointments remain. */
    public void delete(String ownerId, String petId) {
        getOwned(ownerId, petId);
        for (Appointment a : appointments.findByOwnerId(ownerId)) {
            if (a.getPetId().equals(petId) && a.isBooked()) {
                a.setStatus(Appointment.CANCELLED);
                appointments.save(a);
            }
        }
        pets.deleteById(petId);
    }

    private void validate(String name, String species, int age) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Pet name is required.");
        if (species == null || species.isBlank()) throw new IllegalArgumentException("Species is required.");
        if (age < 0 || age > 40) throw new IllegalArgumentException("Age must be between 0 and 40.");
    }

    private String clean(String s) {
        return s == null ? "" : s.trim();
    }
}
