package br.com.abba.soft.mymoney.infrastructure.persistence.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

import br.com.abba.soft.mymoney.infrastructure.persistence.entity.WhatsAppIncomingMessageDocument;
import br.com.abba.soft.mymoney.infrastructure.persistence.entity.WhatsAppMessageStatus;

public interface WhatsAppIncomingMessageRepository extends MongoRepository<WhatsAppIncomingMessageDocument, String> {
    List<WhatsAppIncomingMessageDocument> findTop50ByStatusOrderByReceivedAtAsc(WhatsAppMessageStatus status);
}
