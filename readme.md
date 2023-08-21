# ZoomView

Zoom- and scalable ImageView

## public fields

### minScale, maxScale (default 1.0, 6.0)
sets the minimum and maximum possible scale factor

### isScrollLimited (default true)
limits scrolling and scaling to the borders of the view

### isScalable (default true)
enables/disables scaling

### isScrollable (default true)
enables/disables scrolling

### scaleMode (default: SCALE_FIT_CENTER)
default scale mode on view start and double click
SCALE_CENTER centers image
SCALE_FIT_CENTER centers image and scales the image to fit in the view

## public methods

### setVectorDrawable(id : Int)
sets an vector drawable as imageBitmap

## Usage with glide library

```kotlin
fun loadImageToImageView(context: Context, url: String, view: ImageView) {

    //Don't use this
    Glide
        .with(context)
        .load(url)
        .into(view)
    
    //Use that
    Glide
        .with(context)
        .asBitmap()
        .load(url)
        .into(object : CustomTarget<Bitmap>() {
            override fun onResourceReady(
                resource: Bitmap,
                transition: Transition<in Bitmap>?
            ) {
                view.setImageBitmap(resource)
            }

            override fun onLoadCleared(placeholder: Drawable?) {}

        })
}
```








