package ec.edu.espe.banquito.accountservice.converter;

import ec.edu.espe.banquito.accountservice.enums.MovementType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class MovementTypeConverter implements AttributeConverter<MovementType, String> {

    @Override
    public String convertToDatabaseColumn(MovementType attribute) {
        if (attribute == null) {
            return null;
        }
        return switch (attribute) {
            case DEBIT -> "DEBITO";
            case CREDIT -> "CREDITO";
        };
    }

    @Override
    public MovementType convertToEntityAttribute(String dbData) {
        return MovementType.fromDatabaseValue(dbData);
    }
}
