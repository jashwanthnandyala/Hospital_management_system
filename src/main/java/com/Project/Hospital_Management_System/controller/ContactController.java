package com.Project.Hospital_Management_System.controller;

import com.Project.Hospital_Management_System.common.api.ApiResponse;
import com.Project.Hospital_Management_System.dto.ContactCreateRequest;
import com.Project.Hospital_Management_System.entity.PatientContact;
import com.Project.Hospital_Management_System.repository.PatientContactRepository;
import com.Project.Hospital_Management_System.service.PatientContactService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients/{patientId}/contacts")
@PreAuthorize("hasAnyRole('PATIENT','DOCTOR','ADMIN')")
public class ContactController {

    private final PatientContactService contactService;
    private final PatientContactRepository contactRepository;

    public ContactController(PatientContactService contactService,
                             PatientContactRepository contactRepository) {
        this.contactService = contactService;
        this.contactRepository = contactRepository;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> add(@PathVariable Long patientId,
                                                  @RequestBody ContactCreateRequest req) {
        contactService.addContact(
                patientId, req.name, req.relationship, req.phone, req.email,
                req.primary != null && req.primary
        );
        return ResponseEntity.ok(ApiResponse.ok("Contact added successfully."));
    }

    @GetMapping
    public List<PatientContact> list(@PathVariable Long patientId) {
        return contactRepository.findByPatient_Id(patientId);
    }

    @GetMapping("/primary")
    public ResponseEntity<PatientContact> primary(@PathVariable Long patientId) {
        return contactRepository.findByPatient_IdAndPrimaryTrue(patientId)
                .stream().findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{contactId}")
    public ResponseEntity<ApiResponse<Void>> update(@PathVariable Long patientId,
                                                     @PathVariable Long contactId,
                                                     @RequestBody ContactCreateRequest req) {
        contactService.updateContact(
                patientId, contactId, req.name, req.relationship, req.phone, req.email,
                req.primary != null && req.primary
        );
        return ResponseEntity.ok(ApiResponse.ok("Contact updated successfully."));
    }

    @DeleteMapping("/{contactId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long patientId,
                                                     @PathVariable Long contactId) {
        contactService.deleteContact(patientId, contactId);
        return ResponseEntity.ok(ApiResponse.ok("Contact deleted successfully."));
    }
}
