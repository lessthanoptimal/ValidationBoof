
Marker PDF Generation
opencv_contrib-4.5.3/modules/aruco/misc/pattern_generator

Instructions:
- Set up venv and install dependencies
- Modify MarkerPrinter to support 6X6_100
- rm arucoDictBytesList.npz
- python MarkerPrinter.py --generate arucoDictBytesList.npz

python MarkerPrinter.py --charuco --file "./charuco.pdf" --charuco_dictionary DICT_6X6_100 --square_length 0.03 --marker_length 0.02 --border_bits 1 --size_x 6 --size_y 8

python MarkerPrinter.py --aruco_grid --file "./aruco_grid.pdf" --charuco_dictionary DICT_6X6_100 --marker_separation 0.008 --marker_length 0.03 --border_bits 1 --size_x 5 --size_y 7
