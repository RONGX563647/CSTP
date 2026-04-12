package com.aisale.backend.dto;

import cn.hutool.core.bean.BeanUtil;
import com.aisale.backend.entity.Address;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressResponse {

    private Long id;
    private String receiverName;
    private String receiverPhone;
    private String province;
    private String city;
    private String district;
    private String detailAddress;
    private Boolean isDefault;
    private String fullAddress;

    public static AddressResponse fromEntity(Address address) {
        AddressResponse response = BeanUtil.copyProperties(address, AddressResponse.class);
        response.setFullAddress(address.getProvince() + address.getCity() +
                               address.getDistrict() + address.getDetailAddress());
        return response;
    }
}