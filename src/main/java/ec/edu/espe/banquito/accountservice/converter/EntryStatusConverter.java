package ec.edu.espe.banquito.accountservice.converter;

import ec.edu.espe.banquito.accountservice.enums.EntryStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EntryStatusConverter implements AttributeConverter<EntryStatus, String> {

    @Override
    public String convertToDatabaseColumn(EntryStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return switch (attribute) {
            case REGISTERED -> "REGISTRADO";
            case CANCELLED -> "ANULADO";
        };
    }

    @Override
    public EntryStatus convertToEntityAttribute(String dbData) {
        return EntryStatus.fromDatabaseValue(dbData);
    }
}
