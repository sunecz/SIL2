# SIL2 (Simple Image Library v2) #
**SIL2** is an experimental simple image library written in Java. It contains useful methods for doing image processing, color conversions and more. Note that the library uses Java's internal loading and saving of images, for more information see Java's `ImageIO` class. Also the primary class for images is the JavaFX's Image class.

*What happened to the first version?* Well, it was so bad it was rewritten.

## I/O operations

### Loading images
```java
Image image = FXImageIO.open([File | Path | InputStream]);
```

### Saving images
```java
FXImageIO.save(image, outputFormat, [File | Path | InputStream]);
```

### Creating JavaFX Image of a desired format
```java
Image image = FXImage.create(javaFXPixelFormat, pixelsBuffer, width, height);
```

## Conversion

### Converting between AWT & JavaFX
`SwingFXUtils` can be slow sometimes, you can use `FXAWT` class to speed it up a little. It uses faster algorithms for conversion and tries to keep the same image format, if possible.
```java
// AWT -> FX
Image fxImage = FXAWT.fxImage(awtImage);
// FX -> AWT
BufferedImage awtImage = FXAWT.awtImage(fxImage);
```

## Native image
Native image is an image that uses the Operating system's pixel format internally. That is, when used, no conversion is made when updating the JavaFX image, thus speeding the process.

### Creating a native image
```java
// Without existing pixels
Image image = NativeImage.create(width, height);
// With existing pixels
Image image = NativeImage.create(width, height, pixels);
```

### Ensuring an Image is a native one
```java
Image nativeImage = NativeImage.ensurePixelFormat(image);
```

### What's my native pixel format?
```java
PixelFormat<?> nativePixelFormat = NativeImage.getNativePixelFormat();
```
