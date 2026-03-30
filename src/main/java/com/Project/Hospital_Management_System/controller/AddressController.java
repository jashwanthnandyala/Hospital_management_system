package com.Project.Hospital_Management_System.controller;

import com.Project.Hospital_Management_System.common.api.ApiResponse;
import com.Project.Hospital_Management_System.dto.AddressCreateRequest;
import com.Project.Hospital_Management_System.entity.PatientAddress;
import com.Project.Hospital_Management_System.repository.PatientAddressRepository;
import com.Project.Hospital_Management_System.service.PatientAddressService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients/{patientId}/addresses")
@PreAuthorize("hasAnyRole('PATIENT','DOCTOR','ADMIN')")
public class AddressController {

    private final PatientAddressService addressService;
    private final PatientAddressRepository addressRepository;

    public AddressController(PatientAddressService addressService,
                             PatientAddressRepository addressRepository) {
        this.addressService = addressService;
        this.addressRepository = addressRepository;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> add(@PathVariable Long patientId,
                                                  @RequestBody AddressCreateRequest req) {
        addressService.addAddress(
                patientId, req.addressType, req.line1, req.line2, req.city, req.state,
                req.postalCode, req.country != null ? req.country : "IN",
                req.primary != null && req.primary
        );
        return ResponseEntity.ok(ApiResponse.ok("Address added successfully."));
    }

    @GetMapping
    public List<PatientAddress> list(@PathVariable Long patientId) {
        return addressRepository.findByPatient_Id(patientId);
    }

    @GetMapping("/primary")
    public ResponseEntity<PatientAddress> primary(@PathVariable Long patientId) {
        return addressRepository.findByPatient_IdAndPrimaryTrue(patientId)
                .stream().findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<ApiResponse<Void>> update(@PathVariable Long patientId,
                                                     @PathVariable Long addressId,
                                                     @RequestBody AddressCreateRequest req) {
        addressService.updateAddress(
                patientId, addressId, req.addressType, req.line1, req.line2, req.city, req.state,
                req.postalCode, req.country != null ? req.country : "IN",
                req.primary != null && req.primary
        );
        return ResponseEntity.ok(ApiResponse.ok("Address updated successfully."));
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long patientId,
                                                     @PathVariable Long addressId) {
        addressService.deleteAddress(patientId, addressId);
        return ResponseEntity.ok(ApiResponse.ok("Address deleted successfully."));
    }
}
