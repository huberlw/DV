extern "C"

#define max(a, b) (a > b)? a: b
#define min(a, b) (a > b)? b: a


__global__ void MergerHelper1(double *seedHBMax, double *seedHBMin, double *mergingHBMaxes, double *mergingHBMins, double *combinedMax, double *combinedMin, double *opClassPnts, int *toBeDeleted, int numDims, int numMergingHBs, int cases)
{
    int n = blockIdx.x * blockDim.x + threadIdx.x;
    if (n < numMergingHBs)
    {
        int offset = n * numDims;
        for (int i = 0; i < numDims; i++)
        {
            combinedMax[i+offset] = max(seedHBMax[i], mergingHBMaxes[i+offset]);
            combinedMin[i+offset] = min(seedHBMin[i], mergingHBMins[i+offset]);
        }

        // 1 = do merge, 0 = do not merge
        int merge = 1;
        for (int i = 0; i < cases; i += numDims)
        {
            bool withinSpace = true;
            for (int j = 0; j < numDims; j++)
            {
                if (!(opClassPnts[i+j] <= combinedMax[j+offset] && opClassPnts[i+j] >= combinedMin[j+offset]))
                {
                    withinSpace = false;
                    break;
                }
            }

            if (withinSpace)
            {
                merge = 0;
                break;
            }
        }

        toBeDeleted[n] = merge;
    }
}
