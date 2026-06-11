package ${package}.domain.valueobject;

import lombok.Value;
import org.apache.commons.lang3.StringUtils;

/**
 * value object
 * @author hanfeng
 */
@Value
public class Address implements ValueObject<Address> {

    String country;
    String province;
    String city;
    String district;
    String street;
    String zipCode;

    public Address(String country, String province, String city, String district, String street, String zipCode) {
        if (StringUtils.isBlank(country)) {
            throw new IllegalArgumentException("Country must not be empty");
        }
        if (StringUtils.isBlank(province)) {
            throw new IllegalArgumentException("Province must not be empty");
        }
        if (StringUtils.isBlank(city)) {
            throw new IllegalArgumentException("City must not be empty");
        }

        this.country = country;
        this.province = province;
        this.city = city;
        this.district = district;
        this.street = street;
        this.zipCode = zipCode;
    }

    /**
     * get full
     */
    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        sb.append(country).append(" ");
        sb.append(province).append(" ");
        sb.append(city).append(" ");
        if (StringUtils.isNotBlank(district)) {
            sb.append(district).append(" ");
        }
        if (StringUtils.isNotBlank(street)) {
            sb.append(street);
        }
        return sb.toString().trim();
    }

    @Override
    public boolean sameValueAs(Address other) {
        return other != null &&
                this.country.equals(other.country) &&
                this.province.equals(other.province) &&
                this.city.equals(other.city) &&
                StringUtils.equals(this.district, other.district) &&
                StringUtils.equals(this.street, other.street) &&
                StringUtils.equals(this.zipCode, other.zipCode);
    }
}
