package com.itsworld.assistant.service;

final class AssistantPhotos {
    private AssistantPhotos() {}

    static boolean owned(String mediaBase, String bucket, Long user, String photo) {
        String prefix = mediaBase.replaceAll("/+$", "") + "/" + bucket + "/assistant/" + user + "/";
        return photo != null && photo.startsWith(prefix)
                && photo.substring(prefix.length()).matches("[0-9]{8}/[a-f0-9]{32}\\.(jpg|jpeg|png|gif|webp)");
    }
}
