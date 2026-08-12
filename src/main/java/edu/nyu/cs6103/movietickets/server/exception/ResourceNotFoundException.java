package edu.nyu.cs6103.movietickets.server.exception;

/** Thrown when an entity requested by the client does not exist. */
public class ResourceNotFoundException extends RuntimeException {

    private final String resourceType;
    private final Object resourceId;

    public ResourceNotFoundException(String resourceType, Object resourceId) {
        super(buildMessage(resourceType, resourceId));
        this.resourceType = requireResourceType(resourceType);
        this.resourceId = requireResourceId(resourceId);
    }

    public String resourceType() {
        return resourceType;
    }

    public Object resourceId() {
        return resourceId;
    }

    private static String buildMessage(String resourceType, Object resourceId) {
        return requireResourceType(resourceType) + " not found: " + requireResourceId(resourceId);
    }

    private static String requireResourceType(String resourceType) {
        if (resourceType == null || resourceType.isBlank()) {
            throw new IllegalArgumentException("resourceType must not be blank");
        }
        return resourceType.trim();
    }

    private static Object requireResourceId(Object resourceId) {
        if (resourceId == null) {
            throw new IllegalArgumentException("resourceId must not be null");
        }
        return resourceId;
    }
}
