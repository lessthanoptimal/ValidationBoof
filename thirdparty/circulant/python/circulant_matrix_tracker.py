
from __future__ import print_function


import pylab
import scipy.misc

debug = False

class CirculantMatrixTracker:

    def __init__(self, should_resize_image):
        # parameters according to the paper --
        self.padding = 1.0  # extra area surrounding the target
        #spatial bandwidth (proportional to target)
        self.output_sigma_factor = 1 / float(16)
        self.sigma = 0.2  # gaussian kernel bandwidth
        self.lambda_value = 1e-2  # regularization
        # linear interpolation factor for adaptation
        self.interpolation_factor = 0.075

        self.should_resize_image = should_resize_image

        self.z = None
        self.alphaf = None
        self.response = None

    def initialize(self, image, pos , target_sz ):
        if len(image.shape) == 3 and image.shape[2] > 1:
            image = rgb2gray(image)
        self.image = image
        if self.should_resize_image:
            self.image = scipy.misc.imresize(self.image, 0.5)
            self.image = self.image / 255.0

        # window size, taking padding into account
        self.sz = pylab.floor(target_sz * (1 + self.padding))
        self.pos = pos

        # desired output (gaussian shaped), bandwidth proportional to target size
        output_sigma = pylab.sqrt(pylab.prod(self.sz)) * self.output_sigma_factor

        grid_y = pylab.arange(self.sz[0]) - pylab.floor(self.sz[0]/2)
        grid_x = pylab.arange(self.sz[1]) - pylab.floor(self.sz[1]/2)
        #[rs, cs] = ndgrid(grid_x, grid_y)
        rs, cs = pylab.meshgrid(grid_x, grid_y)
        self.y = pylab.exp(-0.5 / output_sigma**2 * (rs**2 + cs**2))
        self.yf = pylab.fft2(self.y)

        # store pre-computed cosine window
        self.cos_window = pylab.outer(pylab.hanning(self.sz[0]),
                                      pylab.hanning(self.sz[1]))

        # get subwindow at current estimated target position,
        # to train classifer
        x = get_subwindow(self.image, self.pos, self.sz, self.cos_window)

        # Kernel Regularized Least-Squares,
        # calculate alphas (in Fourier domain)
        k = dense_gauss_kernel(self.sigma, x)
        self.alphaf = pylab.divide(self.yf, (pylab.fft2(k) + self.lambda_value))  # Eq. 7
        self.z = x

        return

    def find(self, image):
        if len(image.shape) == 3 and image.shape[2] > 1:
            image = rgb2gray(image)
        self.image = image
        if self.should_resize_image:
            self.image = scipy.misc.imresize(self.image, 0.5)
            self.image = self.image / 255.0

        # get subwindow at current estimated target position,
        # to train classifer
        x = get_subwindow(self.image, self.pos, self.sz, self.cos_window)

        # calculate response of the classifier at all locations
        k = dense_gauss_kernel(self.sigma, x, self.z)
        kf = pylab.fft2(k)
        alphaf_kf = pylab.multiply(self.alphaf, kf)
        self.response = pylab.real(pylab.ifft2(alphaf_kf))  # Eq. 9

        # target location is at the maximum response
        r = self.response
        self.row, self.col = pylab.unravel_index(r.argmax(), r.shape)
        self.pos = self.pos - pylab.floor(self.sz/2) + [self.row, self.col]

        return self.pos

    def update_template(self ):
        """
        Update the tracking template,
        new_example is expected to match the size of
        the example provided to the constructor
        """

        # get subwindow at current estimated target position,
        # to train classifer
        x = get_subwindow(self.image, self.pos, self.sz, self.cos_window)

        # Kernel Regularized Least-Squares,
        # calculate alphas (in Fourier domain)
        k = dense_gauss_kernel(self.sigma, x)
        new_alphaf = pylab.divide(self.yf, (pylab.fft2(k) + self.lambda_value))  # Eq. 7
        new_z = x

        # subsequent frames, interpolate model
        f = self.interpolation_factor
        self.alphaf = (1 - f) * self.alphaf + f * new_alphaf
        self.z = (1 - f) * self.z + f * new_z

        return

def rgb2gray(rgb_image):
    "Based on http://stackoverflow.com/questions/12201577"
    # [0.299, 0.587, 0.144] normalized gives [0.29, 0.57, 0.14]
    return pylab.dot(rgb_image[:, :, :3], [0.29, 0.57, 0.14])


def get_subwindow(im, pos, sz, cos_window):
    """
    Obtain sub-window from image, with replication-padding.
    Returns sub-window of image IM centered at POS ([y, x] coordinates),
    with size SZ ([height, width]). If any pixels are outside of the image,
    they will replicate the values at the borders.

    The subwindow is also normalized to range -0.5 .. 0.5, and the given
    cosine window COS_WINDOW is applied
    (though this part could be omitted to make the function more general).
    """

    if pylab.isscalar(sz):  # square sub-window
        sz = [sz, sz]

    ys = pylab.floor(pos[0]) \
        + pylab.arange(sz[0], dtype=int) - pylab.floor(sz[0]/2)
    xs = pylab.floor(pos[1]) \
        + pylab.arange(sz[1], dtype=int) - pylab.floor(sz[1]/2)

    ys = ys.astype(int)
    xs = xs.astype(int)

    # check for out-of-bounds coordinates,
    # and set them to the values at the borders
    ys[ys < 0] = 0
    ys[ys >= im.shape[0]] = im.shape[0] - 1

    xs[xs < 0] = 0
    xs[xs >= im.shape[1]] = im.shape[1] - 1
    #zs = range(im.shape[2])

    # extract image
    #out = im[pylab.ix_(ys, xs, zs)]
    out = im[pylab.ix_(ys, xs)]

    if debug:
        print("Out max/min value==", out.max(), "/", out.min())
        pylab.figure()
        pylab.imshow(out, cmap=pylab.cm.gray)
        pylab.title("cropped subwindow")

    #pre-process window --
    # normalize to range -0.5 .. 0.5
    # pixels are already in range 0 to 1
    out = out.astype(pylab.float64) - 0.5

    # apply cosine window
    out = pylab.multiply(cos_window, out)

    return out


def dense_gauss_kernel(sigma, x, y=None):
    """
    Gaussian Kernel with dense sampling.
    Evaluates a gaussian kernel with bandwidth SIGMA for all displacements
    between input images X and Y, which must both be MxN. They must also
    be periodic (ie., pre-processed with a cosine window). The result is
    an MxN map of responses.

    If X and Y are the same, omit the third parameter to re-use some
    values, which is faster.
    """

    xf = pylab.fft2(x)  # x in Fourier domain
    x_flat = x.flatten()
    xx = pylab.dot(x_flat.transpose(), x_flat)  # squared norm of x

    if y is not None:
        # general case, x and y are different
        yf = pylab.fft2(y)
        y_flat = y.flatten()
        yy = pylab.dot(y_flat.transpose(), y_flat)
    else:
        # auto-correlation of x, avoid repeating a few operations
        yf = xf
        yy = xx

    # cross-correlation term in Fourier domain
    xyf = pylab.multiply(xf, pylab.conj(yf))

    # to spatial domain
    xyf_ifft = pylab.ifft2(xyf)
    #xy_complex = circshift(xyf_ifft, floor(x.shape/2))
    row_shift, col_shift = pylab.floor(pylab.array(x.shape)/2).astype(int)
    xy_complex = pylab.roll(xyf_ifft, row_shift, axis=0)
    xy_complex = pylab.roll(xy_complex, col_shift, axis=1)
    xy = pylab.real(xy_complex)

    # calculate gaussian response for all positions
    scaling = -1 / (sigma**2)
    xx_yy = xx + yy
    xx_yy_2xy = xx_yy - 2 * xy
    k = pylab.exp(scaling * pylab.maximum(0, xx_yy_2xy / x.size))

    #print("dense_gauss_kernel x.shape ==", x.shape)
    #print("dense_gauss_kernel k.shape ==", k.shape)

    return k


# end of file
