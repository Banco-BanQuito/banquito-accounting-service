package ec.edu.espe.banquito.accountservice.converter;

import ec.edu.espe.banquito.accountservice.enums.AccountType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AccountTypeConverter implements AttributeConverter<AccountType, String> {

    @Override
    public String convertToDatabaseColumn(AccountType attribute) {
        if (attribute == null) {
            return null;
        }
        return switch (attribute) {
            case STRUCTURAL -> "ESTRUCTURAL";
            case DETAIL -> "DETALLE";
        };
    }

    @Override
    public AccountType convertToEntityAttribute(String dbData) {
        return AccountType.fromDatabaseValue(dbData);
    }
}
