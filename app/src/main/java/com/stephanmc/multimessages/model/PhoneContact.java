package com.stephanmc.multimessages.model;

/**
 * Simple POJO representing a contact in Multi Messages
 */
public class PhoneContact {

    private String id;
    private String mContactName;
    private String mContactNumber;
    private boolean mIsSelected;
    private String mPhotoURI;

    public String getContactName() {
        return mContactName;
    }

    public PhoneContact setContactName(String contactName) {
        mContactName = contactName;
        return this;
    }

    public String getContactNumber() {
        return mContactNumber;
    }

    public void setContactNumber(String contactNumber) {
        mContactNumber = contactNumber;
    }

    public boolean isSelected() {
        return mIsSelected;
    }

    public void setSelected(boolean isSelected) {
        this.mIsSelected = isSelected;
    }

    public String getPhotoURI() {
        return mPhotoURI;
    }

    public void setPhotoURI(String photoURI) {
        mPhotoURI = photoURI;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof PhoneContact && ((PhoneContact) obj).getId().equals(getId());
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
