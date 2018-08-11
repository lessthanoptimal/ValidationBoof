#include <cstddef>
#include <string>
#include <iostream>
#include <iterator>
#include <boost/program_options.hpp>
#include <boost/filesystem.hpp>

#include "ceres_bal.hpp"
#include "ceres/ceres.h"

using namespace boost::algorithm;
using namespace std;
using namespace ceres;
using namespace ceres::examples;
namespace po = boost::program_options;
namespace bf = boost::filesystem;

DEFINE_string(trust_region_strategy, "levenberg_marquardt",
              "Options are: levenberg_marquardt, dogleg.");
DEFINE_string(dogleg, "traditional_dogleg", "Options are: traditional_dogleg,"
                                            "subspace_dogleg.");
DEFINE_bool(inner_iterations, false, "Use inner iterations to non-linearly "
                                     "refine each successful trust region step.");
DEFINE_string(blocks_for_inner_iterations, "automatic", "Options are: "
                                                        "automatic, cameras, points, cameras,points, points,cameras");
DEFINE_string(linear_solver, "sparse_schur", "Options are: "
                                             "sparse_schur, dense_schur, iterative_schur, sparse_normal_cholesky, "
                                             "dense_qr, dense_normal_cholesky and cgnr.");
DEFINE_bool(explicit_schur_complement, false, "If using ITERATIVE_SCHUR "
                                              "then explicitly compute the Schur complement.");
DEFINE_string(preconditioner, "jacobi", "Options are: "
                                        "identity, jacobi, schur_jacobi, cluster_jacobi, "
                                        "cluster_tridiagonal.");
DEFINE_string(visibility_clustering, "canonical_views",
              "single_linkage, canonical_views");
DEFINE_string(sparse_linear_algebra_library, "suite_sparse",
              "Options are: suite_sparse and cx_sparse.");
DEFINE_string(dense_linear_algebra_library, "eigen",
              "Options are: eigen and lapack.");
DEFINE_string(ordering, "automatic", "Options are: automatic, user.");
DEFINE_bool(use_quaternions, false, "If true, uses quaternions to represent "
                                    "rotations. If false, angle axis is used.");
DEFINE_bool(use_local_parameterization, false, "For quaternions, use a local "
                                               "parameterization.");
DEFINE_bool(robustify, false, "Use a robust loss function.");
DEFINE_double(eta, 1e-2, "Default value for eta. Eta determines the "
                         "accuracy of each linear solve of the truncated newton step. "
                         "Changing this parameter can affect solve performance.");
DEFINE_int32(num_threads, 1, "Number of threads.");
DEFINE_int32(num_iterations, 5, "Number of iterations.");
DEFINE_double(max_solver_time, 1e32, "Maximum solve time in seconds.");
DEFINE_bool(nonmonotonic_steps, false, "Trust region algorithm can use"
                                       " nonmonotic steps.");
DEFINE_double(rotation_sigma, 0.0, "Standard deviation of camera rotation "
                                   "perturbation.");
DEFINE_double(translation_sigma, 0.0, "Standard deviation of the camera "
                                      "translation perturbation.");
DEFINE_double(point_sigma, 0.0, "Standard deviation of the point "
                                "perturbation.");
DEFINE_int32(random_seed, 38401, "Random seed used to set the state "
                                 "of the pseudo random number generator used to generate "
                                 "the pertubations.");
DEFINE_bool(line_search, false, "Use a line search instead of trust region "
                                "algorithm.");
DEFINE_bool(mixed_precision_solves, false, "Use mixed precision solves.");
DEFINE_int32(max_num_refinement_iterations, 50, "Iterative refinement iterations");

void SetLinearSolver(Solver::Options* options) {
    CHECK(StringToLinearSolverType(FLAGS_linear_solver,
                                   &options->linear_solver_type));
    CHECK(StringToPreconditionerType(FLAGS_preconditioner,
                                     &options->preconditioner_type));
    CHECK(StringToVisibilityClusteringType(FLAGS_visibility_clustering,
                                           &options->visibility_clustering_type));
    CHECK(StringToSparseLinearAlgebraLibraryType(
            FLAGS_sparse_linear_algebra_library,
            &options->sparse_linear_algebra_library_type));
    CHECK(StringToDenseLinearAlgebraLibraryType(
            FLAGS_dense_linear_algebra_library,
            &options->dense_linear_algebra_library_type));
    options->use_explicit_schur_complement = FLAGS_explicit_schur_complement;
//    options->use_mixed_precision_solves = FLAGS_mixed_precision_solves;
    options->max_num_iterations = FLAGS_max_num_refinement_iterations;
}

void SetOrdering(BALProblem* bal_problem, Solver::Options* options) {
    const int num_points = bal_problem->num_points();
    const int point_block_size = bal_problem->point_block_size();
    double* points = bal_problem->mutable_points();
    const int num_cameras = bal_problem->num_cameras();
    const int camera_block_size = bal_problem->camera_block_size();
    double* cameras = bal_problem->mutable_cameras();
    if (options->use_inner_iterations) {
        if (FLAGS_blocks_for_inner_iterations == "cameras") {
            LOG(INFO) << "Camera blocks for inner iterations";
            options->inner_iteration_ordering.reset(new ParameterBlockOrdering);
            for (int i = 0; i < num_cameras; ++i) {
                options->inner_iteration_ordering->AddElementToGroup(cameras + camera_block_size * i, 0);
            }
        } else if (FLAGS_blocks_for_inner_iterations == "points") {
            LOG(INFO) << "Point blocks for inner iterations";
            options->inner_iteration_ordering.reset(new ParameterBlockOrdering);
            for (int i = 0; i < num_points; ++i) {
                options->inner_iteration_ordering->AddElementToGroup(points + point_block_size * i, 0);
            }
        } else if (FLAGS_blocks_for_inner_iterations == "cameras,points") {
            LOG(INFO) << "Camera followed by point blocks for inner iterations";
            options->inner_iteration_ordering.reset(new ParameterBlockOrdering);
            for (int i = 0; i < num_cameras; ++i) {
                options->inner_iteration_ordering->AddElementToGroup(cameras + camera_block_size * i, 0);
            }
            for (int i = 0; i < num_points; ++i) {
                options->inner_iteration_ordering->AddElementToGroup(points + point_block_size * i, 1);
            }
        } else if (FLAGS_blocks_for_inner_iterations == "points,cameras") {
            LOG(INFO) << "Point followed by camera blocks for inner iterations";
            options->inner_iteration_ordering.reset(new ParameterBlockOrdering);
            for (int i = 0; i < num_cameras; ++i) {
                options->inner_iteration_ordering->AddElementToGroup(cameras + camera_block_size * i, 1);
            }
            for (int i = 0; i < num_points; ++i) {
                options->inner_iteration_ordering->AddElementToGroup(points + point_block_size * i, 0);
            }
        } else if (FLAGS_blocks_for_inner_iterations == "automatic") {
            LOG(INFO) << "Choosing automatic blocks for inner iterations";
        } else {
            LOG(FATAL) << "Unknown block type for inner iterations: "
                       << FLAGS_blocks_for_inner_iterations;
        }
    }
    // Bundle adjustment problems have a sparsity structure that makes
    // them amenable to more specialized and much more efficient
    // solution strategies. The SPARSE_SCHUR, DENSE_SCHUR and
    // ITERATIVE_SCHUR solvers make use of this specialized
    // structure.
    //
    // This can either be done by specifying Options::ordering_type =
    // ceres::SCHUR, in which case Ceres will automatically determine
    // the right ParameterBlock ordering, or by manually specifying a
    // suitable ordering vector and defining
    // Options::num_eliminate_blocks.
    if (FLAGS_ordering == "automatic") {
        return;
    }
    auto * ordering = new ceres::ParameterBlockOrdering;
    // The points come before the cameras.
    for (int i = 0; i < num_points; ++i) {
        ordering->AddElementToGroup(points + point_block_size * i, 0);
    }
    for (int i = 0; i < num_cameras; ++i) {
        // When using axis-angle, there is a single parameter block for
        // the entire camera.
        ordering->AddElementToGroup(cameras + camera_block_size * i, 1);
    }
    options->linear_solver_ordering.reset(ordering);
}
void SetMinimizerOptions(Solver::Options* options) {
    options->max_num_iterations = FLAGS_num_iterations;
    options->minimizer_progress_to_stdout = true;
    options->num_threads = FLAGS_num_threads;
    options->eta = FLAGS_eta;
    options->max_solver_time_in_seconds = FLAGS_max_solver_time;
    options->use_nonmonotonic_steps = FLAGS_nonmonotonic_steps;
    if (FLAGS_line_search) {
        options->minimizer_type = ceres::LINE_SEARCH;
    }
    CHECK(StringToTrustRegionStrategyType(FLAGS_trust_region_strategy,
                                          &options->trust_region_strategy_type));
    CHECK(StringToDoglegType(FLAGS_dogleg, &options->dogleg_type));
    options->use_inner_iterations = FLAGS_inner_iterations;
}

void SetSolverOptionsFromFlags(BALProblem* bal_problem,
                               Solver::Options* options) {
    SetMinimizerOptions(options);
    SetLinearSolver(options);
    SetOrdering(bal_problem, options);
}

void BuildProblem(BALProblem* bal_problem, Problem* problem) {
    const int point_block_size = bal_problem->point_block_size();
    const int camera_block_size = bal_problem->camera_block_size();
    double* points = bal_problem->mutable_points();
    double* cameras = bal_problem->mutable_cameras();
    // Observations is 2*num_observations long array observations =
    // [u_1, u_2, ... , u_n], where each u_i is two dimensional, the x
    // and y positions of the observation.
    const double* observations = bal_problem->observations();
    for (int i = 0; i < bal_problem->num_observations(); ++i) {
        CostFunction* cost_function;
        // Each Residual block takes a point and a camera as input and
        // outputs a 2 dimensional residual.
        cost_function =
                (FLAGS_use_quaternions)
                ? SnavelyReprojectionErrorWithQuaternions::Create(
                        observations[2 * i + 0],
                        observations[2 * i + 1])
                : SnavelyReprojectionError::Create(
                        observations[2 * i + 0],
                        observations[2 * i + 1]);
        // If enabled use Huber's loss function.
        LossFunction* loss_function = FLAGS_robustify ? new HuberLoss(1.0) : NULL;
        // Each observation correponds to a pair of a camera and a point
        // which are identified by camera_index()[i] and point_index()[i]
        // respectively.
        double* camera =
                cameras + camera_block_size * bal_problem->camera_index()[i];
        double* point = points + point_block_size * bal_problem->point_index()[i];
        problem->AddResidualBlock(cost_function, loss_function, camera, point);
    }
    if (FLAGS_use_quaternions && FLAGS_use_local_parameterization) {
        auto camera_parameterization = new ProductParameterization(
                        new QuaternionParameterization(),
                        new IdentityParameterization(6));
        for (int i = 0; i < bal_problem->num_cameras(); ++i) {
            problem->SetParameterization(cameras + camera_block_size * i,
                                         (LocalParameterization*)camera_parameterization);
        }
    }
}

void process_in_the_large( const string& input_path , const string& output_path ) {
    cout << "Loading: " << input_path << endl;

    BALProblem bal_problem = BALProblem(input_path,false);

    Problem problem;
    srand(FLAGS_random_seed);

    cout << "Normalizing" << endl;
    bal_problem.Normalize();

    BuildProblem(&bal_problem, &problem);
    Solver::Options options;
    SetSolverOptionsFromFlags(&bal_problem, &options);
    options.gradient_tolerance = 1e-16;
    options.function_tolerance = 1e-16;
    Solver::Summary summary;

    cout << "Solving" << endl;
    Solve(options, &problem, &summary);
    std::cout << summary.FullReport() << "\n";

    // TODO write out to a BAL file
}

int main( int argc, char *argv[] ) {
    try {

        po::options_description desc("Allowed options");
        desc.add_options()
            ("help", "produce help message")
            ("Input,I", po::value<std::string >(), "input path")
            ("Output,O", po::value<std::string >(), "output path");

        po::variables_map vm;
        po::store(po::parse_command_line(argc, argv, desc), vm);
        po::notify(vm);

        if (vm.count("help")) {
            cout << desc << "\n";
            return 0;
        }

        if ( !vm.count("Input")) {
            cout << desc << "\n";
            cout << "Input path was not set.\n";
            return 0;
        }
        if ( !vm.count("Output")) {
            cout << desc << "\n";
            cout << "Output path was not set.\n";
            return 0;
        }
        process_in_the_large(vm["Input"].as<string>(),vm["Output"].as<string>());
    } catch(exception& e) {
        cerr << "error: " << e.what() << "\n";
        return 1;
    } catch(...) {
        cerr << "Exception of unknown type!\n";
    }

    printf("done!\n");
    return 0;
}