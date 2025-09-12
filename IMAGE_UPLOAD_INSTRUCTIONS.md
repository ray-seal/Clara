# Image Upload Feature - Usage Instructions

## Overview
The Clara app now supports image uploads with posts in the Public Feed Activity. Users can:
- Select images from their gallery
- Preview selected images before posting
- Upload images to Firebase Storage
- View images in the post feed

## Setup Requirements

### Firebase Storage Configuration
1. **Enable Firebase Storage** in your Firebase console:
   - Go to Firebase Console → Storage
   - Click "Get Started" and follow the setup
   - Set up Security Rules (see below)

2. **Firebase Storage Security Rules**:
```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /post_images/{allPaths=**} {
      allow read: if true;
      allow write: if request.auth != null;
    }
  }
}
```

### Android App Setup
1. **Dependencies Added**:
   - Firebase Storage: `implementation("com.google.firebase:firebase-storage:20.3.0")`
   - Glide for image loading: `implementation("com.github.bumptech.glide:glide:4.16.0")`

2. **Permissions Added**:
   - `READ_EXTERNAL_STORAGE` for gallery access
   - `CAMERA` for camera access (future enhancement)

## How to Use

### For Users
1. **Creating a Post with Image**:
   - Open the app and navigate to Public Feed
   - Type your message (optional if you have an image)
   - Tap "📷 Add Image" to select from gallery
   - Select one or more categories
   - Tap "Post" to upload

2. **Image Preview**:
   - Selected images are shown in a preview area
   - Tap "❌" to remove the selected image

3. **Posting**:
   - Posts can be text-only, image-only, or both
   - At least one category must be selected
   - The "Post" button shows "Posting..." during upload

### For Developers

#### Key Files Modified:
- `PublicFeedActivity.java` - Main image upload logic
- `Post.java` - Added imageUrl field
- `PostAdapter.java` - Image display in feed
- `activity_public_feed.xml` - Image picker UI
- `item_post.xml` - Image display layout
- `AndroidManifest.xml` - Permissions
- `app/build.gradle.kts` - Dependencies

#### Firebase Storage Structure:
```
gs://your-project.appspot.com/
└── post_images/
    ├── userId_timestamp1.jpg
    ├── userId_timestamp2.jpg
    └── ...
```

#### Error Handling:
- Permission requests for gallery access
- Image upload failure handling  
- Network connectivity issues
- Invalid image selection

## Testing
1. Run the app in an Android emulator or device
2. Navigate to Public Feed
3. Try creating posts with and without images
4. Verify images appear correctly in the feed
5. Test with different image sizes and formats

## Future Enhancements
- Multiple image support per post
- Camera integration for taking photos
- Image compression before upload
- Offline image caching
- Image editing capabilities

## Troubleshooting

### Common Issues:
1. **Images not uploading**: Check Firebase Storage rules and authentication
2. **Images not displaying**: Verify Glide dependency and network connectivity  
3. **Permission denied**: Ensure READ_EXTERNAL_STORAGE permission is granted
4. **Build errors**: Clean project and rebuild, ensure all dependencies are properly added

### Debug Steps:
1. Check Firebase console for uploaded images
2. Review Android logs for error messages
3. Verify Firebase Storage rules allow read/write access
4. Test with different image formats (JPEG, PNG)