package dev.langgraph.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class FileBasedCheckpointStore implements CheckpointStore {
    
    private final Path storageDirectory;
    private final ObjectMapper objectMapper;

    public FileBasedCheckpointStore(Path storageDirectory) throws PersistenceException {
        this(storageDirectory, new JacksonStateSerializer().getObjectMapper());
    }

    public FileBasedCheckpointStore(Path storageDirectory, ObjectMapper objectMapper) throws PersistenceException {
        this.storageDirectory = Objects.requireNonNull(storageDirectory, "Storage directory cannot be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "ObjectMapper cannot be null");
        
        try {
            Files.createDirectories(storageDirectory);
        } catch (IOException e) {
            throw new PersistenceException("Failed to create storage directory", e);
        }
    }

    @Override
    public void save(Checkpoint checkpoint) throws PersistenceException {
        Path checkpointFile = getCheckpointPath(checkpoint.checkpointId());
        try {
            objectMapper.writeValue(checkpointFile.toFile(), checkpoint);
        } catch (IOException e) {
            throw new PersistenceException("Failed to save checkpoint: " + checkpoint.checkpointId(), e);
        }
    }

    @Override
    public Optional<Checkpoint> load(String checkpointId) throws PersistenceException {
        Path checkpointFile = getCheckpointPath(checkpointId);
        if (!Files.exists(checkpointFile)) {
            return Optional.empty();
        }
        
        try {
            Checkpoint checkpoint = objectMapper.readValue(checkpointFile.toFile(), Checkpoint.class);
            return Optional.of(checkpoint);
        } catch (IOException e) {
            throw new PersistenceException("Failed to load checkpoint: " + checkpointId, e);
        }
    }

    @Override
    public List<Checkpoint> listByExecutionId(String executionId) throws PersistenceException {
        return listAllCheckpoints().stream()
                .filter(c -> c.executionId().equals(executionId))
                .sorted(Comparator.comparing(Checkpoint::timestamp))
                .collect(Collectors.toList());
    }

    @Override
    public List<Checkpoint> listByGraphId(String graphId) throws PersistenceException {
        return listAllCheckpoints().stream()
                .filter(c -> c.graphId().equals(graphId))
                .sorted(Comparator.comparing(Checkpoint::timestamp))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Checkpoint> getLatestByExecutionId(String executionId) throws PersistenceException {
        return listByExecutionId(executionId).stream()
                .max(Comparator.comparing(Checkpoint::timestamp));
    }

    @Override
    public void delete(String checkpointId) throws PersistenceException {
        Path checkpointFile = getCheckpointPath(checkpointId);
        try {
            Files.deleteIfExists(checkpointFile);
        } catch (IOException e) {
            throw new PersistenceException("Failed to delete checkpoint: " + checkpointId, e);
        }
    }

    @Override
    public void deleteByExecutionId(String executionId) throws PersistenceException {
        List<Checkpoint> checkpoints = listByExecutionId(executionId);
        for (Checkpoint checkpoint : checkpoints) {
            delete(checkpoint.checkpointId());
        }
    }

    @Override
    public void clear() throws PersistenceException {
        try (Stream<Path> paths = Files.list(storageDirectory)) {
            paths.filter(p -> p.toString().endsWith(".json"))
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            // Log but continue
                        }
                    });
        } catch (IOException e) {
            throw new PersistenceException("Failed to clear checkpoints", e);
        }
    }

    @Override
    public boolean exists(String checkpointId) {
        return Files.exists(getCheckpointPath(checkpointId));
    }

    private Path getCheckpointPath(String checkpointId) {
        return storageDirectory.resolve(checkpointId + ".json");
    }

    private List<Checkpoint> listAllCheckpoints() throws PersistenceException {
        try (Stream<Path> paths = Files.list(storageDirectory)) {
            return paths.filter(p -> p.toString().endsWith(".json"))
                    .map(p -> {
                        try {
                            return objectMapper.readValue(p.toFile(), Checkpoint.class);
                        } catch (IOException e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new PersistenceException("Failed to list checkpoints", e);
        }
    }

    public Path getStorageDirectory() {
        return storageDirectory;
    }
}
