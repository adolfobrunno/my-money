package br.com.abba.soft.mymoney.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Configuration
public class MongoConfig {

    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();
        converters.add(new ZonedDateTimeWriteConverter());
        converters.add(new ZonedDateTimeReadConverter());
        return new MongoCustomConversions(converters);
    }

    @WritingConverter
    static class ZonedDateTimeWriteConverter implements Converter<ZonedDateTime, Date> {
        @Override
        public Date convert(ZonedDateTime source) {
            if (source == null) return null;
            return Date.from(source.toInstant());
        }
    }

    @ReadingConverter
    static class ZonedDateTimeReadConverter implements Converter<Date, ZonedDateTime> {
        @Override
        public ZonedDateTime convert(Date source) {
            if (source == null) return null;
            // Restore in system default zone. Adjust if you want a fixed zone.
            return ZonedDateTime.ofInstant(source.toInstant(), ZoneId.systemDefault());
        }
    }
}
