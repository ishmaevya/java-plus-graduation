package ru.practicum.kafka.serializer;

import java.io.ByteArrayOutputStream;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

public class GeneralAvroSerializer implements Serializer<SpecificRecordBase> {

    @Override
    public void configure(java.util.Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public byte[] serialize(String topic, SpecificRecordBase data) {
        if (data == null) {
            return null;
        }
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            DatumWriter<SpecificRecordBase> writer = new SpecificDatumWriter<>(data.getSchema());
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(output, null);
            writer.write(data, encoder);
            encoder.flush();
            return output.toByteArray();
        } catch (Exception e) {
            throw new SerializationException("Failed to serialize Avro record", e);
        }
    }
}
