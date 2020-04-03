# SIL2 (Simple Image Library v2) #
**SIL2** is an experimental simple image library written in Java. It contains useful methods for doing image processing, color conversions and more. The primary class for images is the JavaFX's `Image` class.

Note that this library uses Java's internal loading and saving of images, hence supporting only those image formats that are supported by Java itself. For more information see Java's `ImageIO` class.

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

## `IImage` class
The main image class used for all image processing. It encapsulates all important methods for doing image convolution and image processing actions.

Note: this class uses the internal image's buffer. Therefore, if an IImage that will not affect the original image needs to be created, you can use `ImageUtils.copy(image)` to copy the image before passing it as an argument to the constructor.

### Creating a new IImage
```java
// By default the IImage is double buffered
IImage<?> iimg = new IImage<>(image);
// Another number of buffers
IImage<?> iimg = new IImage<>(image, numberOfBuffers);
// Custom buffer strategy
IImage<?> iimg = new IImage<>(image, bufferStrategyFactory);
```

### Applying various operations to the image
To apply operations (such as contrast, blur, etc.) use the `applyOperation(operation)` method. Pre-defined operations that can be used are defined in `sune.lib.sil2.operation.*`, those are `Adjustments`, `Filters`, `Effects`, `Morphology`, `Transforms`, and `ImageOperations`.

```java
IImage<?> iimg = ...;
iimg.applyOperation(new Filters.BoxBlur<>(strengthBlur));
iimg.applyOperation(new Adjustments.Contrast<>(strengthContrast));
iimg.applyOperation(new MyCustomIImageOperation<>());
```

An operation can also return a value:
```java
float optimalThreshold = iimg.applyOperation(new ImageOperations.OtsuOptimalThreshold<>());
```

### Applying actions
There are currently 4 types of actions:
| Type | Explanation                                                            |
|------|------------------------------------------------------------------------|
| INT  | Works only with indicies                                               |
| RGB  | Works with indicies and RGBA array (red, green, blue, alpha)           |
| HSL  | Works with indicies and HSLA array (hue, saturation, lightness, alpha) |
| HCL  | Works with indicies and HCLA array (hue, chroma, lightness, alpha)     |

```java
// INT
iimg.applyActionINT((input, output, index, varStore) -> {
    format.setARGB(output, index, format.getARGB(input, index) & mask);
});
// RGB
iimg.applyActionRGB((rgb, input, output, index, varStore) -> {
    int r, g, b, a;
    // ...
    rgb[0] = r;
    rgb[1] = g;
    rgb[2] = b;
    rgb[3] = a;
});
// HSL
iimg.applyActionHSL((hsl, input, output, index, varStore) -> {
    float h, s, l, a;
    // ...
    hsl[0] = h;
    hsl[1] = s;
    hsl[2] = l;
    hsl[3] = a;
});
// HCL
iimg.applyActionHCL((hcl, input, output, index, varStore) -> {
    float h, c, l, a;
    // ...
    hsl[0] = h;
    hsl[1] = c;
    hsl[2] = l;
    hsl[3] = a;
});
```

### Image convolution
To do an image convolution with a kernel, you can use the `convolute2d` method. There is currently available only the 2D version of this method. Note that the kernel **MUST** be normalized.
```java
// Apply the Mean filter
float[] kernel = {
    0.1f, 0.1f, 0.1f,
    0.1f, 0.1f, 0.1f,
    0.1f, 0.1f, 0.1f
};
iimg.convolute2d(kernel, numberOfIterations, includeAlphaChannel);
```

### Individual pixels
You can also get and set individual pixels of the image.
```java
iimg.setPixel(x, y, colorInARGB);
iimg.getPixel(x, y);
```

## Color conversions
This library supports converting to and from these color formats: `RGB`, `HSL`, `HCL`, `LAB`, `XYZ`. To convert a color to another format, use the `Colors` class, or if a specific format (e.g. `BGRA`) is required the `FormatColor` class, or the `NativeColor` class for the native format.
```java
// RGB -> HSL
float[] hsl = new float[3];
Colors.rgb2hsl(red, green, blue, hsl);
// Red in BGRA format
int bgraRed = new FormatColor(BGRAImagePixelFormat.INSTANCE).rgb(255, 0, 0);
// Red in Native format
int nativeRed = NativeColor.get().rgb(255, 0, 0);
```

## Image Utilities
To crop, rotate, resize, combine, ... an image, you can use the `ImageUtils` class that contains methods for these operations.
```java
// Crop an image
Image image = ImageUtils.crop(image, x, y, width, height);
// Resize an image
Image image = ImageUtils.fastresize(image, width, height);
// Rotate an image
Image image = ImageUtils.fastrotate(image, rad);
// ...
```
*Most methods are also available for buffers, these do not create a new image object but rather output directly to a given buffer.*

## Matrix Utilities
To do operations with a matrix (kernel), you can use the `MatrixUtils` class.
```java
float[] kernel = { /* ... */ };
MatrixUtils.rotate(kernel, rad);
```
*Currently there is only the rotate() method that in-place rotates a matrix.*

## Other utilities
There are some more utility classes that can be used for various tasks.

### BufferUtils
Used to copy or fill a buffer.
```java
// Copy a buffer to another buffer
BufferUtils.buffercopy(source, destination);
// Fill a buffer with an int value
BufferUtils.fill(buffer, value, step);
```

### Pixels
Used to copy a pixels region.
```java
Pixels.copy(source, sourceX, sourceY, sourceStride, destination, destinationX, destinationY, destinationStride, width, height, elementsPerPixel);
```

### FPSUtils
Used to measure FPS in JavaFX.
```
float fps = FPSUtils.getFPS();
```

### FastMath
Used to do math stuff faster.
```java
float sin = FastMath.sin(rad);
float cos = FastMath.cos(rad);
float pow = FastMath.pow(a, b);
// ...
```

## License
This library is published under the MIT license (https://github.com/sunecz/SIL2/blob/master/license.md).
