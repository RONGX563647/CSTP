package com.aisale.backend.controller;

import com.aisale.backend.dto.ApiResponse;
import com.aisale.backend.dto.AddressRequest;
import com.aisale.backend.dto.AddressResponse;
import com.aisale.backend.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "用户地址", description = "用户地址管理接口")
@RestController
@RequestMapping("/api/user/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @Operation(summary = "获取用户所有地址")
    @GetMapping
    public ApiResponse<List<AddressResponse>> getAllAddresses(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            List<AddressResponse> addresses = addressService.getUserAddresses(userDetails.getUsername());
            return ApiResponse.success(addresses);
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @Operation(summary = "获取单个地址详情")
    @GetMapping("/{id}")
    public ApiResponse<AddressResponse> getAddressById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        try {
            AddressResponse address = addressService.getAddressById(userDetails.getUsername(), id);
            return ApiResponse.success(address);
        } catch (RuntimeException e) {
            return ApiResponse.error(404, e.getMessage());
        }
    }

    @Operation(summary = "添加新地址")
    @PostMapping
    public ApiResponse<AddressResponse> createAddress(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AddressRequest request) {
        try {
            AddressResponse address = addressService.createAddress(userDetails.getUsername(), request);
            return ApiResponse.success("地址添加成功", address);
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @Operation(summary = "更新地址")
    @PutMapping("/{id}")
    public ApiResponse<AddressResponse> updateAddress(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody AddressRequest request) {
        try {
            AddressResponse address = addressService.updateAddress(userDetails.getUsername(), id, request);
            return ApiResponse.success("地址更新成功", address);
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @Operation(summary = "删除地址")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteAddress(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        try {
            addressService.deleteAddress(userDetails.getUsername(), id);
            return ApiResponse.success("地址删除成功", null);
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @Operation(summary = "设置默认地址")
    @PutMapping("/{id}/default")
    public ApiResponse<AddressResponse> setDefaultAddress(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        try {
            AddressResponse address = addressService.setDefaultAddress(userDetails.getUsername(), id);
            return ApiResponse.success("默认地址设置成功", address);
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }
}