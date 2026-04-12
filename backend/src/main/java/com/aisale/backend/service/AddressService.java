package com.aisale.backend.service;

import cn.hutool.core.bean.BeanUtil;
import com.aisale.backend.dto.AddressRequest;
import com.aisale.backend.dto.AddressResponse;
import com.aisale.backend.entity.Address;
import com.aisale.backend.entity.User;
import com.aisale.backend.exception.business.ConflictException;
import com.aisale.backend.exception.business.NotFoundException;
import com.aisale.backend.repository.AddressRepository;
import com.aisale.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public List<AddressResponse> getUserAddresses(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("用户不存在"));

        List<Address> addresses = addressRepository.findByUserAndStatusOrderByIsDefaultDescCreatedAtDesc(
                user, Address.AddressStatus.ACTIVE);

        return addresses.stream()
                .map(AddressResponse::fromEntity)
                .toList();
    }

    public AddressResponse getAddressById(String username, Long addressId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("用户不存在"));

        Address address = addressRepository.findByUserAndIdAndStatus(user, addressId, Address.AddressStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("地址不存在"));

        return AddressResponse.fromEntity(address);
    }

    @Transactional
    public AddressResponse createAddress(String username, AddressRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("用户不存在"));

        long addressCount = addressRepository.countByUserAndStatus(user, Address.AddressStatus.ACTIVE);
        if (addressCount >= 10) {
            throw new RuntimeException("最多只能添加10个地址");
        }

        Address address = BeanUtil.copyProperties(request, Address.class);
        address.setUser(user);

        if (request.getIsDefault() || addressCount == 0) {
            clearDefaultAddress(user);
            address.setIsDefault(true);
        }

        address = addressRepository.save(address);
        return AddressResponse.fromEntity(address);
    }

    @Transactional
    public AddressResponse updateAddress(String username, Long addressId, AddressRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("用户不存在"));

        Address address = addressRepository.findByUserAndIdAndStatus(user, addressId, Address.AddressStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("地址不存在"));

        BeanUtil.copyProperties(request, address);

        if (request.getIsDefault()) {
            clearDefaultAddress(user);
            address.setIsDefault(true);
        } else if (address.getIsDefault()) {
            address.setIsDefault(false);
            setFirstAddressAsDefault(user);
        }

        address = addressRepository.save(address);
        return AddressResponse.fromEntity(address);
    }

    @Transactional
    public void deleteAddress(String username, Long addressId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("用户不存在"));

        Address address = addressRepository.findByUserAndIdAndStatus(user, addressId, Address.AddressStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("地址不存在"));

        boolean wasDefault = address.getIsDefault();
        address.setStatus(Address.AddressStatus.DELETED);
        addressRepository.save(address);

        if (wasDefault) {
            setFirstAddressAsDefault(user);
        }
    }

    @Transactional
    public AddressResponse setDefaultAddress(String username, Long addressId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("用户不存在"));

        Address address = addressRepository.findByUserAndIdAndStatus(user, addressId, Address.AddressStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("地址不存在"));

        clearDefaultAddress(user);
        address.setIsDefault(true);
        address = addressRepository.save(address);

        return AddressResponse.fromEntity(address);
    }

    private void clearDefaultAddress(User user) {
        List<Address> addresses = addressRepository.findByUserAndStatusOrderByIsDefaultDescCreatedAtDesc(
                user, Address.AddressStatus.ACTIVE);
        for (Address addr : addresses) {
            if (addr.getIsDefault()) {
                addr.setIsDefault(false);
                addressRepository.save(addr);
            }
        }
    }

    private void setFirstAddressAsDefault(User user) {
        List<Address> addresses = addressRepository.findByUserAndStatusOrderByIsDefaultDescCreatedAtDesc(
                user, Address.AddressStatus.ACTIVE);
        if (!addresses.isEmpty()) {
            Address firstAddress = addresses.get(0);
            firstAddress.setIsDefault(true);
            addressRepository.save(firstAddress);
        }
    }
}