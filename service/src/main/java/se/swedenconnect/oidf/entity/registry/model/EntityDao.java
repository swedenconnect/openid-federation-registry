package se.swedenconnect.oidf.entity.registry.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * EntityDao is a JPA entity representing a database table for storing entities
 * as JSON objects with the objects Subject value as key.
 *
 * @author David Goldring
 */
@Getter
@Setter
@Entity
@ToString
@Table(name = "entities", uniqueConstraints = @UniqueConstraint(columnNames = "subject"))
public class EntityDao {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private long id;

  @Column(unique = true)
  private String subject;

  @Column(columnDefinition = "TEXT")
  private String entity;
}
