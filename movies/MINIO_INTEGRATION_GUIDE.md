# Hướng Dẫn Tích Hợp MinIO - Movies Service

## 📋 Tổng Quan

MinIO đã được tích hợp vào Movies Service để lưu trữ hình ảnh poster của phim. MinIO là một object storage server tương thích với Amazon S3 API, cho phép lưu trữ và quản lý file hiệu quả.

## 🚀 Cài Đặt MinIO

### Option 1: Sử Dụng Docker (Khuyến nghị)

```bash
# Chạy MinIO container
docker run -d \
  --name minio \
  -p 9000:9000 \
  -p 9001:9001 \
  -e MINIO_ROOT_USER=minioadmin \
  -e MINIO_ROOT_PASSWORD=minioadmin \
  -v minio_data:/data \
  quay.io/minio/minio server /data --console-address ":9001"
```

### Option 2: Cài Đặt Trực Tiếp

#### Windows:

```bash
# Download MinIO
curl -O https://dl.min.io/server/minio/release/windows-amd64/minio.exe

# Chạy MinIO
.\minio.exe server C:\minio-data --console-address ":9001"
```

#### Linux/Mac:

```bash
# Download MinIO
wget https://dl.min.io/server/minio/release/linux-amd64/minio
chmod +x minio

# Chạy MinIO
./minio server /mnt/data --console-address ":9001"
```

### Truy Cập MinIO Console

- **Console URL**: http://localhost:9001
- **Username**: `minioadmin`
- **Password**: `minioadmin`

## ⚙️ Cấu Hình

### application.properties

```properties
# MinIO Configuration
minio.endpoint=http://localhost:9000
minio.access-key=minioadmin
minio.secret-key=minioadmin
minio.bucket-name=cinema-movies
minio.image-size.max=5242880  # 5MB
```

### Cấu Hình Tùy Chỉnh

- `minio.endpoint`: URL của MinIO server
- `minio.access-key`: Access key (username)
- `minio.secret-key`: Secret key (password)
- `minio.bucket-name`: Tên bucket lưu trữ
- `minio.image-size.max`: Kích thước file tối đa (bytes)

## 📡 API Endpoints

### 1. Upload Poster

**Upload hình ảnh poster cho movie:**

```http
POST /api/v1/upload/poster
Content-Type: multipart/form-data

Body:
- file: (binary) - Image file
```

**Response:**

```json
{
  "statusCode": 200,
  "message": "Upload poster thành công",
  "data": {
    "url": "http://localhost:9000/cinema-movies/uuid-filename.jpg",
    "filename": "original-filename.jpg"
  }
}
```

**cURL Example:**

```bash
curl -X POST http://localhost:9001/api/v1/upload/poster \
  -F "file=@/path/to/image.jpg"
```

### 2. Delete Poster

**Xóa hình ảnh poster:**

```http
DELETE /api/v1/upload/poster?url={posterUrl}
```

**Response:**

```json
{
  "statusCode": 200,
  "message": "Xóa poster thành công",
  "data": null
}
```

**cURL Example:**

```bash
curl -X DELETE "http://localhost:9001/api/v1/upload/poster?url=http://localhost:9000/cinema-movies/uuid-filename.jpg"
```

## 🎬 Luồng Sử Dụng với Movie

### Tạo Movie Mới với Poster

**Bước 1: Upload Poster**

```bash
curl -X POST http://localhost:9001/api/v1/upload/poster \
  -F "file=@avatar.jpg"
```

**Response:**

```json
{
  "data": {
    "url": "http://localhost:9000/cinema-movies/123e4567-e89b-12d3.jpg",
    "filename": "avatar.jpg"
  }
}
```

**Bước 2: Tạo Movie với posterUrl**

```bash
curl -X POST http://localhost:9001/api/v1/movies \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Avatar 3",
    "description": "The return of Na'\''vi",
    "duration": 180,
    "posterUrl": "http://localhost:9000/cinema-movies/123e4567-e89b-12d3.jpg"
  }'
```

### Cập Nhật Movie với Poster Mới

**Bước 1: Upload Poster Mới**

```bash
curl -X POST http://localhost:9001/api/v1/upload/poster \
  -F "file=@new-poster.jpg"
```

**Bước 2: Update Movie**

```bash
curl -X PUT http://localhost:9001/api/v1/movies/{movieId} \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Avatar 3",
    "description": "Updated description",
    "duration": 180,
    "posterUrl": "http://localhost:9000/cinema-movies/new-uuid.jpg"
  }'
```

> **Lưu ý:** Poster cũ sẽ tự động bị xóa khỏi MinIO

### Xóa Movie

```bash
curl -X DELETE http://localhost:9001/api/v1/movies/{movieId}
```

> **Lưu ý:** Poster sẽ tự động bị xóa khỏi MinIO khi xóa movie

## 🔒 Validation Rules

### File Upload Constraints

- **File Type**: Chỉ chấp nhận image files (image/\*)

  - Supported: JPG, JPEG, PNG, GIF, WebP, SVG
  - Not Supported: PDF, DOC, ZIP, etc.

- **File Size**: Tối đa 5MB (5,242,880 bytes)

  - Có thể thay đổi trong `application.properties`

- **File Name**: Tự động generate UUID để tránh trùng lặp

### Error Messages

```json
// File empty
{
  "code": "FILE_UPLOAD_ERROR",
  "message": "File is empty",
  "status": 400
}

// File too large
{
  "code": "FILE_UPLOAD_ERROR",
  "message": "File size exceeds maximum limit of 5242880 bytes",
  "status": 400
}

// Invalid file type
{
  "code": "FILE_UPLOAD_ERROR",
  "message": "Only image files are allowed",
  "status": 400
}
```

## 🧪 Testing với Postman

### 1. Tạo Collection

Import hoặc tạo mới collection với các requests sau:

#### Upload Poster

```
POST {{base_url}}/api/v1/upload/poster
Body: form-data
  - file: [Select File]
```

#### Create Movie

```
POST {{base_url}}/api/v1/movies
Body: raw (JSON)
{
  "title": "Test Movie",
  "description": "Test Description",
  "duration": 120,
  "posterUrl": "{{poster_url}}"
}
```

### 2. Environment Variables

```
base_url: http://localhost:9001
poster_url: (set from upload response)
```

### 3. Test Scripts

**Upload Poster - Tests Tab:**

```javascript
// Lưu URL vào biến environment
pm.test("Status code is 200", function () {
  pm.response.to.have.status(200);
});

let response = pm.response.json();
pm.environment.set("poster_url", response.data.url);
```

## 📊 MinIO Management

### Web Console

Truy cập MinIO Console tại: http://localhost:9001

**Các chức năng:**

- 📁 Browse buckets và objects
- ⬆️ Upload files manually
- 🗑️ Delete files
- 👥 Quản lý users và permissions
- 📈 Xem metrics và logs

### Bucket Policy (Optional)

Nếu muốn cho phép public access (không cần authentication):

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": { "AWS": ["*"] },
      "Action": ["s3:GetObject"],
      "Resource": ["arn:aws:s3:::cinema-movies/*"]
    }
  ]
}
```

## 🐛 Troubleshooting

### 1. MinIO không kết nối được

**Triệu chứng:**

```
Failed to initialize MinIO client
```

**Giải pháp:**

- Kiểm tra MinIO server đã chạy chưa: `docker ps` hoặc check process
- Verify endpoint trong application.properties
- Check firewall/port 9000 có mở không

### 2. Upload thất bại

**Triệu chứng:**

```
Failed to upload file: Access Denied
```

**Giải pháp:**

- Kiểm tra access-key và secret-key
- Verify bucket đã được tạo
- Check bucket permissions

### 3. Bucket không tồn tại

**Triệu chứng:**

```
Bucket does not exist
```

**Giải pháp:**

- Application tự động tạo bucket khi start
- Hoặc tạo manual qua MinIO Console
- Bucket name: `cinema-movies`

### 4. File không tìm thấy khi delete

**Triệu chứng:**

```
Failed to delete file: The specified key does not exist
```

**Giải pháp:**

- File đã bị xóa trước đó
- Warning log sẽ được ghi nhưng không ảnh hưởng flow
- Xem lại URL có đúng format không

## 🔐 Security Best Practices

### Production Environment

1. **Thay đổi credentials mặc định:**

```properties
minio.access-key=your-secure-access-key
minio.secret-key=your-secure-secret-key-at-least-32-chars
```

2. **Sử dụng HTTPS:**

```properties
minio.endpoint=https://minio.yourdomain.com
```

3. **Cấu hình CORS (nếu cần):**

```bash
mc alias set myminio http://localhost:9000 minioadmin minioadmin
mc cors set myminio cinema-movies --cors-config cors.json
```

4. **Backup bucket:**

```bash
mc mirror myminio/cinema-movies /backup/cinema-movies
```

## 📈 Performance Tips

1. **CDN Integration**: Sử dụng CloudFront hoặc CDN khác để cache images
2. **Image Optimization**: Nén ảnh trước khi upload
3. **Lazy Loading**: Load images khi cần thiết
4. **Thumbnail Generation**: Tạo thumbnail cho large images

## 🎯 Kết Luận

MinIO đã được tích hợp thành công vào Movies Service với các tính năng:

✅ Upload poster images  
✅ Auto-delete old posters khi update  
✅ Auto-delete posters khi delete movie  
✅ File validation (type, size)  
✅ Unique filename generation  
✅ Error handling

**Next Steps:**

- Implement image resizing/optimization
- Add CDN integration
- Implement thumbnail generation
- Add batch upload support

---

**Version:** 1.0.0  
**Last Updated:** 2026-01-13  
**MinIO Version:** 8.5.7
