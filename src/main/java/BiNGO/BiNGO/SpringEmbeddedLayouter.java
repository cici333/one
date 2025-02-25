package BiNGO.BiNGO ;

// redistributed Cytoscape code

/* * Redistributed Date: Mar.25.2005
 * * by : Steven Maere
 * * Copyright (c) 2005 Flanders Interuniversitary Institute for Biotechnology (VIB)
 * *
 * * This program is free software; you can redistribute it and/or modify
 * * it under the terms of the GNU General Public License as published by
 * * the Free Software Foundation; either version 2 of the License, or
 * * (at your option) any later version.
 * *
 * * This program is distributed in the hope that it will be useful,
 * * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 * * The software and documentation provided hereunder is on an "as is" basis,
 * * and the Flanders Interuniversitary Institute for Biotechnology
 * * has no obligations to provide maintenance, support,
 * * updates, enhancements or modifications.  In no event shall the
 * * Flanders Interuniversitary Institute for Biotechnology
 * * be liable to any party for direct, indirect, special,
 * * incidental or consequential damages, including lost profits, arising
 * * out of the use of this software and its documentation, even if
 * * the Flanders Interuniversitary Institute for Biotechnology
 * * has been advised of the possibility of such damage. See the
 * * GNU General Public License for more details.
 * *
 * * You should have received a copy of the GNU General Public License
 * * along with this program; if not, write to the Free Software
 * * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * */

import org.cytoscape.work.Task;
import org.cytoscape.work.TaskMonitor;
import java.util.*;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.myappViz.internal.Loader;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;

import java.awt.geom.Point2D;
import java.util.*;

/**
 * An implementation of Kamada and Kawai's spring embedded layout algorithm.
 * Note: this was copied from giny.util because it is being phased out.  Eventually
 * the layout API will be available to use (TODO: remove when layout API is available)
 */
public class SpringEmbeddedLayouter  {

    public static final int DEFAULT_NUM_LAYOUT_PASSES =
            2;
    public static final double DEFAULT_AVERAGE_ITERATIONS_PER_NODE =
            20.0;

    public static final double[] DEFAULT_NODE_DISTANCE_SPRING_SCALARS =
            new double[]{1.0, 1.0};
    public static final double DEFAULT_NODE_DISTANCE_STRENGTH_CONSTANT =
            15.0;
    public static final double DEFAULT_NODE_DISTANCE_REST_LENGTH_CONSTANT =
            200.0;
    public static final double DEFAULT_DISCONNECTED_NODE_DISTANCE_SPRING_STRENGTH =
            .05;
    public static final double DEFAULT_DISCONNECTED_NODE_DISTANCE_SPRING_REST_LENGTH =
            2500.0;

    public static final double[] DEFAULT_ANTICOLLISION_SPRING_SCALARS =
            new double[]{0.0, 1.0};
    public static final double DEFAULT_ANTICOLLISION_SPRING_STRENGTH =
            100.0;

    protected int numLayoutPasses =
            DEFAULT_NUM_LAYOUT_PASSES;
    protected double averageIterationsPerNode =
            DEFAULT_AVERAGE_ITERATIONS_PER_NODE;

    protected double[] nodeDistanceSpringScalars =
            DEFAULT_NODE_DISTANCE_SPRING_SCALARS;
    protected double nodeDistanceStrengthConstant =
            DEFAULT_NODE_DISTANCE_STRENGTH_CONSTANT;
    protected double nodeDistanceRestLengthConstant =
            DEFAULT_NODE_DISTANCE_REST_LENGTH_CONSTANT;
    protected double disconnectedNodeDistanceSpringStrength =
            DEFAULT_DISCONNECTED_NODE_DISTANCE_SPRING_STRENGTH;
    protected double disconnectedNodeDistanceSpringRestLength =
            DEFAULT_DISCONNECTED_NODE_DISTANCE_SPRING_REST_LENGTH;
    protected double[][] nodeDistanceSpringStrengths;
    protected double[][] nodeDistanceSpringRestLengths;

    protected double[] anticollisionSpringScalars =
            DEFAULT_ANTICOLLISION_SPRING_SCALARS;
    protected double anticollisionSpringStrength =
            DEFAULT_ANTICOLLISION_SPRING_STRENGTH;

    protected CyNetworkView graphView;
    protected int nodeCount;
    protected int edgeCount;
    protected int layoutPass;

    protected HashMap<Long, Integer> nodeIndexToMatrixIndexMap; //maps a root graph index to a distance matrix index
    protected TreeMap<Integer, Long> matrixIndexToNodeIndexMap; //maps a distance matrix index to a root graph instance

    // Keep track of progress for monitoring:
    private int maxValue;
    private TaskMonitor taskMonitor = null;
    private boolean interrupted = false;


    public SpringEmbeddedLayouter()
	{
		numLayoutPasses = 2;
		averageIterationsPerNode = 20D;
		nodeDistanceSpringScalars = DEFAULT_NODE_DISTANCE_SPRING_SCALARS;
		nodeDistanceStrengthConstant = 15D;
		nodeDistanceRestLengthConstant = 200D;
		disconnectedNodeDistanceSpringStrength = 0.050000000000000003D;
		disconnectedNodeDistanceSpringRestLength = 2500D;
		anticollisionSpringScalars = DEFAULT_ANTICOLLISION_SPRING_SCALARS;
		anticollisionSpringStrength = 100D;
	}

	public SpringEmbeddedLayouter(CyNetworkView graph_view)
	{
		numLayoutPasses = 2;
		averageIterationsPerNode = 20D;
		nodeDistanceSpringScalars = DEFAULT_NODE_DISTANCE_SPRING_SCALARS;
		nodeDistanceStrengthConstant = 15D;
		nodeDistanceRestLengthConstant = 200D;
		disconnectedNodeDistanceSpringStrength = 0.050000000000000003D;
		disconnectedNodeDistanceSpringRestLength = 2500D;
		anticollisionSpringScalars = DEFAULT_ANTICOLLISION_SPRING_SCALARS;
		anticollisionSpringStrength = 100D;
		setGraphView(graph_view);
		initializeSpringEmbeddedLayouter();
		this.maxValue = nodeCount * (int) averageIterationsPerNode;
	}
    
    

	public void setGraphView(CyNetworkView new_graph_view)
	{
		graphView = new_graph_view;
	}


	


    public View getGraphView() {
        return graphView;
    } // getGraphView()

    
    
    protected void initializeSpringEmbeddedLayouter() {
        // initialize the layouting.
/*        nodeCount = graphView.getNodeViewCount();
        edgeCount = graphView.getEdgeViewCount();
*/
        
        nodeCount = ((CyNetwork)graphView.getModel()).getNodeCount();
		edgeCount = ((CyNetwork)graphView.getModel()).getEdgeCount();
        // Do nothing.
        // TODO: Something?
    } // initializeSpringEmbeddedLayouter()

    public void calculate() {

        /*
          // initialize the layouting.
          nodeCount = graphView.getNodeViewCount();
          edgeCount = graphView.getEdgeViewCount();
          */
        // initialize progress
        int currentProgress = 0;
        this.maxValue = nodeCount * (int) averageIterationsPerNode;

        //initialize the index map
        nodeIndexToMatrixIndexMap = new HashMap();
		matrixIndexToNodeIndexMap = new TreeMap();
		Iterator nodes = ((CyNetwork)graphView.getModel()).getNodeList().iterator();
		for (int count = 0; nodes.hasNext(); count++)
		{
			CyNode n = (CyNode)nodes.next();
			nodeIndexToMatrixIndexMap.put(n.getSUID(), Integer.valueOf(count));
			matrixIndexToNodeIndexMap.put(Integer.valueOf(count), n.getSUID());
		}


        // Stop if all nodes are closer together than this euclidean distance.
        // TODO: Why is this an appropriate threshold?
        double euclidean_distance_threshold =
                (0.5 * (nodeCount + edgeCount));

        // Stop if the potential energy doesn't go down anymore.
        double potential_energy_percent_change_threshold = .001;

        int num_iterations = (int)
                ((nodeCount * averageIterationsPerNode) / numLayoutPasses);

        List partials_list = createPartialsList();
        PotentialEnergy potential_energy = new PotentialEnergy();
        Iterator node_views_iterator;
/*        View node_view;
        PartialDerivatives partials;*/
        PartialDerivatives furthest_node_partials = null;
        double current_progress_temp;
        double setup_progress = 0.0;
        try {
            for (layoutPass = 0; layoutPass < numLayoutPasses; layoutPass++) {

                setupForLayoutPass();

                //System.out.println( " DO Layout Pass " );

                // initialize this layout pass
                potential_energy.reset();
                partials_list.clear();

                // Calculate all node distances.  Keep track of the furthest.
                
                for (Iterator iterator = graphView.getNodeViews().iterator(); iterator.hasNext();)
    			{
    				View node_view = (View)iterator.next();
    				PartialDerivatives partials = new PartialDerivatives(node_view);
    				calculatePartials(partials, null, potential_energy, false);
    				partials_list.add(partials);
    				if (furthest_node_partials == null || partials.euclideanDistance > furthest_node_partials.euclideanDistance)
    					furthest_node_partials = partials;
    			}
                /*node_views_iterator = graphView.getNodeViewsIterator();
                while (node_views_iterator.hasNext()) {
                    node_view = (NodeView) node_views_iterator.next();

                    partials = new PartialDerivatives(node_view);
                    calculatePartials(partials,
                            null,
                            potential_energy,
                            false);
                    partials_list.add(partials);
                    if ((furthest_node_partials == null) ||
                            (partials.euclideanDistance >
                                    furthest_node_partials.euclideanDistance)
                            ) {
                        //  //System.out.println( "P: "+furthest_node_partials.euclideanDistance+" E: "+partials.euclideanDistance );
                        furthest_node_partials = partials;
                    }
                }*/

                // Until num_iterations, or the furthest node is not-so-fur, move the
                // furthest node towards where it wants to be.
                for (int iterations_i = 0;
                     ((iterations_i < num_iterations) &&
                             (furthest_node_partials.euclideanDistance >=
                                     euclidean_distance_threshold));
                     iterations_i++
                        ) {
                    // TODO: REMOVE
                    //System.out.println( "At iteration " + layoutPass + ":" + iterations_i + ", furthest_node_partials is " + furthest_node_partials + "." );
                    furthest_node_partials =
                            moveNode(furthest_node_partials, partials_list, potential_energy);
                    // Calculate Percentage.  This must be a value between 0..100.
                    int percentComplete = (int) (((double) currentProgress / maxValue) * 100);

                    //  Estimate Time Remaining
                    long timeRemaining = maxValue - currentProgress;

                    //  Update the Task Monitor.
                    //  This automatically updates the UI Component w/ progress bar.
                    if (taskMonitor != null) {
                        taskMonitor.setProgress(percentComplete);
                        taskMonitor.setStatusMessage("Calculating network layout: " + currentProgress + "of " + maxValue);
                        //taskMonitor.setEstimatedTimeRemaining(timeRemaining);
                    }
                    if (interrupted) {
                        throw new InterruptedException();
                    }
                    currentProgress++;
                } // End for each iteration, attempt to minimize the total potential
                // energy by moving the node that is furthest from where it should be.
            } // End for each layout pass
        } catch (InterruptedException e) {
        	;
 //           taskMonitor.setException(e, "Layout cancelled");
        }
    } // doLayout()

    
    
    public void resetDoLayout()
	{
		interrupted = false;
	}

	public boolean doLayout(double weightLayout, double goalTotal, double progress, Loader loader)
	{
		nodeCount = ((CyNetwork)graphView.getModel()).getNodeCount();
		edgeCount = ((CyNetwork)graphView.getModel()).getEdgeCount();
		nodeIndexToMatrixIndexMap = new HashMap();
		matrixIndexToNodeIndexMap = new TreeMap();
		Iterator nodes = ((CyNetwork)graphView.getModel()).getNodeList().iterator();
		for (int count = 0; nodes.hasNext(); count++)
		{
			CyNode n = (CyNode)nodes.next();
			nodeIndexToMatrixIndexMap.put(n.getSUID(), Integer.valueOf(count));
			matrixIndexToNodeIndexMap.put(Integer.valueOf(count), n.getSUID());
		}

		double euclidean_distance_threshold = 0.5D * (double)(nodeCount + edgeCount);
		int num_iterations = (int)(((double)nodeCount * averageIterationsPerNode) / (double)numLayoutPasses);
		List partials_list = createPartialsList();
		PotentialEnergy potential_energy = new PotentialEnergy();
		PartialDerivatives furthest_node_partials = null;
		for (layoutPass = 0; layoutPass < numLayoutPasses; layoutPass++)
		{
			setupForLayoutPass();
			potential_energy.reset();
			partials_list.clear();
			for (Iterator iterator = graphView.getNodeViews().iterator(); iterator.hasNext();)
			{
				View node_view = (View)iterator.next();
				PartialDerivatives partials = new PartialDerivatives(node_view);
				calculatePartials(partials, null, potential_energy, false);
				partials_list.add(partials);
				if (furthest_node_partials == null || partials.euclideanDistance > furthest_node_partials.euclideanDistance)
					furthest_node_partials = partials;
			}

			for (int iterations_i = 0; iterations_i < num_iterations && furthest_node_partials.euclideanDistance >= euclidean_distance_threshold; iterations_i++)
			{
				if (interrupted)
				{
					System.err.println("Interrupted: Layouter");
					resetDoLayout();
					return false;
				}
				furthest_node_partials = moveNode(furthest_node_partials, partials_list, potential_energy);
				progress += 100D * (1.0D / (double)(num_iterations * numLayoutPasses)) * (weightLayout / goalTotal);
				if (loader != null)
					loader.setProgress((int)progress, "Laying out");
			}

		}

		resetDoLayout();
		return true;
	}

    /**
     * Called at the beginning of each layoutPass iteration.
     */
    protected void setupForLayoutPass() {
        setupNodeDistanceSprings();
    } // setupForLayoutPass()

    protected void setupNodeDistanceSprings() {
    	
    	
    	 if (this.layoutPass != 0) {
    	      return;
    	    }

    	    this.nodeDistanceSpringRestLengths = new double[this.nodeCount][this.nodeCount];
    	    this.nodeDistanceSpringStrengths = new double[this.nodeCount][this.nodeCount];

    	    if (this.nodeDistanceSpringScalars[this.layoutPass] == 0.0D) {
    	      return;
    	    }

    	    List nodeList = new ArrayList();
    	    Collection<Long> matrixIndices = this.matrixIndexToNodeIndexMap.values();
    	    int i = 0;

    	    for (Long nodeId : matrixIndices) {
    	      nodeList.add(i, ((CyNetwork)this.graphView.getModel()).getNode(nodeId.longValue()));
    	      i++;
    	    }

    	    NodeDistances ind = new NodeDistances(nodeList, (CyNetwork)this.graphView.getModel(), this.nodeIndexToMatrixIndexMap);
    	    int[][] node_distances = ind.calculate();

    	    if (node_distances == null) {
    	      return;
    	    }

    	    double node_distance_strength_constant = this.nodeDistanceStrengthConstant;

    	    double node_distance_rest_length_constant = this.nodeDistanceRestLengthConstant;

    	    for (int node_i = 0; node_i < this.nodeCount; node_i++)
    	      for (int node_j = node_i + 1; node_j < this.nodeCount; node_j++) {
    	        if (node_distances[node_i][node_j] == 2147483647)
    	          this.nodeDistanceSpringRestLengths[node_i][node_j] = this.disconnectedNodeDistanceSpringRestLength;
    	        else {
    	          this.nodeDistanceSpringRestLengths[node_i][node_j] = (node_distance_rest_length_constant * node_distances[node_i][node_j]);
    	        }

    	        this.nodeDistanceSpringRestLengths[node_j][node_i] = this.nodeDistanceSpringRestLengths[node_i][node_j];

    	        if (node_distances[node_i][node_j] == 2147483647)
    	          this.nodeDistanceSpringStrengths[node_i][node_j] = this.disconnectedNodeDistanceSpringStrength;
    	        else {
    	          this.nodeDistanceSpringStrengths[node_i][node_j] = (node_distance_strength_constant / (node_distances[node_i][node_j] * node_distances[node_i][node_j]));
    	        }

    	        this.nodeDistanceSpringStrengths[node_j][node_i] = this.nodeDistanceSpringStrengths[node_i][node_j];
    	      }
       /* // We only have to do this once.
        if (layoutPass != 0) {
            return;
        }

        nodeDistanceSpringRestLengths = new double[nodeCount][nodeCount];
        nodeDistanceSpringStrengths = new double[nodeCount][nodeCount];

        if (nodeDistanceSpringScalars[layoutPass] == 0.0) {
            return;
        }

        //create a list of nodes that has the same indices as the nodeIndexToMatrixIndexMap
        ArrayList nodeList = new ArrayList();
        Collection matrixIndices = matrixIndexToNodeIndexMap.values();
        int i = 0;
        for (Iterator iterator = matrixIndices.iterator(); iterator.hasNext();) {
            Integer nodeIndex = (Integer) iterator.next();
            nodeList.add(i, ((CyNetwork)graphView.getModel()).getNode(nodeIndex.longValue()));
            nodeList.add(i, graphView.getGraphPerspective().getNode(nodeIndex.intValue()));
            i++;
        }
        NodeDistances ind = new NodeDistances(nodeList, (CyNetwork)graphView.getModel(), nodeIndexToMatrixIndexMap);
        NodeDistances ind = new NodeDistances(nodeList, graphView.getGraphPerspective(), nodeIndexToMatrixIndexMap);
        int[][] node_distances = (int[][]) ind.calculate();

        if (node_distances == null) {
            return;
        }

        // TODO: A good strength_constant is the characteristic path length of the
        // graph.  For now we'll just use nodeDistanceStrengthConstant.
        double node_distance_strength_constant = nodeDistanceStrengthConstant;

        // TODO: rest_length_constant can be chosen to scale the whole graph.
        // To make it the size of the current view, try
        // rest_length_constant = Math.sqrt( ( ( graphView.getViewRect().width / graphView.getViewRect.height() ) / 4 ) / graphView.getGraphDiameter() );
        // To make it bigger, try
        // rest_length_constant = graphView.averageEdgeLength();
        // To make it smaller, try
        // rest_length_constant = Math.sqrt( ( graphView.getViewRect().width * graphView.getViewRect.height() ) / graphView.getGraphDiameter() );
        // For now we'll just use nodeDistanceRestLengthConstant.
        double node_distance_rest_length_constant = nodeDistanceRestLengthConstant;

        // Calculate the rest lengths and strengths based on the node distance data
        for (int node_i = 0; node_i < nodeCount; node_i++) {
            for (int node_j = (node_i + 1); node_j < nodeCount; node_j++) {

                //System.out.println( "APSP: node_i: "+node_i+ " node_j: "+ node_j+" == "+node_distances[ node_i ][node_j ] );

                if (node_distances[node_i][node_j] == Integer.MAX_VALUE) {
                    nodeDistanceSpringRestLengths[node_i][node_j] =
                            disconnectedNodeDistanceSpringRestLength;
                    //System.out.println( "disconnectedNodeDistanceSpringRestLength 1: "+ disconnectedNodeDistanceSpringRestLength );
                } else {
                    nodeDistanceSpringRestLengths[node_i][node_j] =
                            (node_distance_rest_length_constant *
                                    node_distances[node_i][node_j]);
                    //System.out.println( " ELSE 1: "+nodeDistanceSpringRestLengths[ node_i ][ node_j ] );
                }
                // Mirror over the diagonal.
                nodeDistanceSpringRestLengths[node_j][node_i] =
                        nodeDistanceSpringRestLengths[node_i][node_j];

                if (node_distances[node_i][node_j] == Integer.MAX_VALUE) {
                    nodeDistanceSpringStrengths[node_i][node_j] =
                            disconnectedNodeDistanceSpringStrength;
                } else {
                    nodeDistanceSpringStrengths[node_i][node_j] =
                            (node_distance_strength_constant /
                                    (node_distances[node_i][node_j] *
                                            node_distances[node_i][node_j])
                            );
                }
                // Mirror over the diagonal.
                nodeDistanceSpringStrengths[node_j][node_i] =
                        nodeDistanceSpringStrengths[node_i][node_j];


            }

        }
        // currentProgress has been increased by ( nodeCount * nodeCount ).
*/
    } // setupNodeDistanceSprings()

    /**
     * If partials_list is given, adjust all partials (bidirectional) for the
     * current location of the given partials and return the new furthest node's
     * partials.  Otherwise, just adjust the given partials (using the
     * graphView's nodeViewsIterator), and return it.  If reversed is true then
     * partials_list must be provided and all adjustments made by a non-reversed
     * call (with the same partials with the same graphNodeView at the same
     * location) will be undone.
     * Complexity is O( #Nodes ).
     */
    protected PartialDerivatives calculatePartials(PartialDerivatives partials,
                                                   List partials_list,
                                                   PotentialEnergy potential_energy,
                                                   boolean reversed) {

        partials.reset();

        View node_view = partials.getNodeView();
        
        
        int node_view_index = ((Integer)nodeIndexToMatrixIndexMap.get(((CyNode)node_view.getModel()).getSUID())).intValue();
		double node_view_radius = ((Double)node_view.getVisualProperty(BasicVisualLexicon.NODE_WIDTH)).doubleValue();
		double node_view_x = ((Double)node_view.getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION)).doubleValue();
		double node_view_y = ((Double)node_view.getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION)).doubleValue();
		
        /*int node_view_index = ((Integer) nodeIndexToMatrixIndexMap.get(
                new Integer(node_view.getRootGraphIndex()))).intValue();
        double node_view_radius = node_view.getWidth();
        double node_view_x = node_view.getXPosition();
        double node_view_y = node_view.getYPosition();
*/
        //System.out.println( "index: "+node_view_index+" x: "+node_view_x+" y:" +node_view_y );

        PartialDerivatives other_node_partials = null;
        View other_node_view;
        int other_node_view_index;
        double other_node_view_radius;

        PartialDerivatives furthest_partials = null;

        Iterator iterator;
        if (partials_list == null) {
        	iterator = graphView.getNodeViews().iterator();
            //iterator = graphView.getNodeViewsIterator();
        } else {
            iterator = partials_list.iterator();
        }
        double delta_x;
        double delta_y;
        double euclidean_distance;
        double euclidean_distance_cubed;
        double distance_from_rest;
        double distance_from_touching;
        double incremental_change;
        while (iterator.hasNext()) {
            if (partials_list == null) {
                other_node_view = (View) iterator.next();
            } else {
                other_node_partials = (PartialDerivatives) iterator.next();
                other_node_view = other_node_partials.getNodeView();
            }

            if (((CyNode)node_view.getModel()).getSUID() == ((CyNode)other_node_view.getModel()).getSUID()){
            //if (node_view.getRootGraphIndex() == other_node_view.getRootGraphIndex()) {
                //System.out.println( "Nodes are the same. " );
                continue;
            }

        
	        other_node_view_index = ((Integer)nodeIndexToMatrixIndexMap.get(((CyNode)other_node_view.getModel()).getSUID())).intValue();
			other_node_view_radius = Math.max(((Double)other_node_view.getVisualProperty(BasicVisualLexicon.NODE_WIDTH)).doubleValue(), ((Double)other_node_view.getVisualProperty(BasicVisualLexicon.NODE_HEIGHT)).doubleValue());
			delta_x = node_view_x - ((Double)other_node_view.getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION)).doubleValue();
			delta_y = node_view_y - ((Double)other_node_view.getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION)).doubleValue();
		
           /* other_node_view_index = ((Integer) nodeIndexToMatrixIndexMap.get( new Integer(other_node_view.getRootGraphIndex()))).intValue();
            other_node_view_radius = other_node_view.getWidth();

            delta_x = (node_view_x - other_node_view.getXPosition());
            delta_y = (node_view_y - other_node_view.getYPosition());
*/
            //System.out.println( "Delta's Calculated: "+delta_y+ "  "+delta_x );

            euclidean_distance =
                    Math.sqrt((delta_x * delta_x) + (delta_y * delta_y));
            euclidean_distance_cubed = Math.pow(euclidean_distance, 3);

            //System.out.println( "Euclidean_Distance: "+euclidean_distance+" Euclidean_Distance_Cubed: "+euclidean_distance_cubed );

            distance_from_touching =
                    (euclidean_distance -
                            (node_view_radius + other_node_view_radius));

            //System.out.println( "Distance_From_Touching: "+distance_from_touching );

            incremental_change =
                    (nodeDistanceSpringScalars[layoutPass] *
                            (nodeDistanceSpringStrengths[node_view_index][other_node_view_index] *
                                    (delta_x -
                                            (
                                                    (nodeDistanceSpringRestLengths[node_view_index][other_node_view_index] *
                                                            delta_x) /
                                                            euclidean_distance
                                            )
                                    )
                            )
                    );

            //System.out.println( "Incremental_Change: "+incremental_change );

            if (!reversed) {
                partials.x += incremental_change;
            }
            if (other_node_partials != null) {
                incremental_change =
                        (nodeDistanceSpringScalars[layoutPass] *
                                (nodeDistanceSpringStrengths[other_node_view_index][node_view_index] *
                                        (-delta_x -
                                                (
                                                        (nodeDistanceSpringRestLengths[other_node_view_index][node_view_index] *
                                                                -delta_x) /
                                                                euclidean_distance
                                                )
                                        )
                                )
                        );
                if (reversed) {
                    other_node_partials.x -= incremental_change;
                } else {
                    other_node_partials.x += incremental_change;
                }
            }
            if (distance_from_touching < 0.0) {
                incremental_change =
                        (anticollisionSpringScalars[layoutPass] *
                                (anticollisionSpringStrength *
                                        (delta_x -
                                                (
                                                        ((node_view_radius + other_node_view_radius) *
                                                                delta_x) /
                                                                euclidean_distance
                                                )
                                        )
                                )
                        );
                if (!reversed) {
                    partials.x += incremental_change;
                }
                if (other_node_partials != null) {
                    incremental_change =
                            (anticollisionSpringScalars[layoutPass] *
                                    (anticollisionSpringStrength *
                                            (-delta_x -
                                                    (
                                                            ((node_view_radius + other_node_view_radius) *
                                                                    -delta_x) /
                                                                    euclidean_distance
                                                    )
                                            )
                                    )
                            );
                    if (reversed) {
                        other_node_partials.x -= incremental_change;
                        //System.out.println( "Other_Node_Partials (-): "+other_node_partials.x );
                    } else {
                        other_node_partials.x += incremental_change;
                        //System.out.println( "Other_Node_Partials (+): "+other_node_partials.x );
                    }
                }
            }
            incremental_change =
                    (nodeDistanceSpringScalars[layoutPass] *
                            (nodeDistanceSpringStrengths[node_view_index][other_node_view_index] *
                                    (delta_y -
                                            (
                                                    (nodeDistanceSpringRestLengths[node_view_index][other_node_view_index] *
                                                            delta_y) /
                                                            euclidean_distance
                                            )
                                    )
                            )
                    );

            //System.out.println( "Incremental_Change: "+incremental_change );

            if (!reversed) {
                partials.y += incremental_change;
            }
            if (other_node_partials != null) {
                incremental_change =
                        (nodeDistanceSpringScalars[layoutPass] *
                                (nodeDistanceSpringStrengths[other_node_view_index][node_view_index] *
                                        (-delta_y -
                                                (
                                                        (nodeDistanceSpringRestLengths[other_node_view_index][node_view_index] *
                                                                -delta_y) /
                                                                euclidean_distance
                                                )
                                        )
                                )
                        );
                if (reversed) {
                    other_node_partials.y -= incremental_change;
                } else {
                    other_node_partials.y += incremental_change;
                }
            }
            if (distance_from_touching < 0.0) {
                incremental_change =
                        (anticollisionSpringScalars[layoutPass] *
                                (anticollisionSpringStrength *
                                        (delta_y -
                                                (
                                                        ((node_view_radius + other_node_view_radius) *
                                                                delta_y) /
                                                                euclidean_distance
                                                )
                                        )
                                )
                        );
                if (!reversed) {
                    partials.y += incremental_change;
                }
                if (other_node_partials != null) {
                    incremental_change =
                            (anticollisionSpringScalars[layoutPass] *
                                    (anticollisionSpringStrength *
                                            (-delta_y -
                                                    (
                                                            ((node_view_radius + other_node_view_radius) *
                                                                    -delta_y) /
                                                                    euclidean_distance
                                                    )
                                            )
                                    )
                            );
                    if (reversed) {
                        other_node_partials.y -= incremental_change;
                    } else {
                        other_node_partials.y += incremental_change;
                    }
                }
            }

            incremental_change =
                    (nodeDistanceSpringScalars[layoutPass] *
                            (nodeDistanceSpringStrengths[node_view_index][other_node_view_index] *
                                    (1.0 -
                                            (
                                                    (nodeDistanceSpringRestLengths[node_view_index][other_node_view_index] *
                                                            (delta_y * delta_y)
                                                    ) /
                                                            euclidean_distance_cubed
                                            )
                                    )
                            )
                    );
            //System.out.println( "Incremental_Change: "+incremental_change );

            if (reversed) {
                if (other_node_partials != null) {
                    other_node_partials.xx -= incremental_change;
                }
            } else {
                partials.xx += incremental_change;
                if (other_node_partials != null) {
                    other_node_partials.xx += incremental_change;
                }
            }
            if (distance_from_touching < 0.0) {
                incremental_change =
                        (anticollisionSpringScalars[layoutPass] *
                                (anticollisionSpringStrength *
                                        (1.0 -
                                                (
                                                        ((node_view_radius + other_node_view_radius) *
                                                                (delta_y * delta_y)
                                                        ) /
                                                                euclidean_distance_cubed
                                                )
                                        )
                                )
                        );
                if (reversed) {
                    if (other_node_partials != null) {
                        other_node_partials.xx -= incremental_change;
                    }
                } else {
                    partials.xx += incremental_change;
                    if (other_node_partials != null) {
                        other_node_partials.xx += incremental_change;
                    }
                }
            }
            incremental_change =
                    (nodeDistanceSpringScalars[layoutPass] *
                            (nodeDistanceSpringStrengths[node_view_index][other_node_view_index] *
                                    (1.0 -
                                            (
                                                    (nodeDistanceSpringRestLengths[node_view_index][other_node_view_index] *
                                                            (delta_x * delta_x)
                                                    ) /
                                                            euclidean_distance_cubed
                                            )
                                    )
                            )
                    );

            //System.out.println( "Incremental_Change: "+incremental_change );

            if (reversed) {
                if (other_node_partials != null) {
                    other_node_partials.yy -= incremental_change;
                }
            } else {
                partials.yy += incremental_change;
                if (other_node_partials != null) {
                    other_node_partials.yy += incremental_change;
                }
            }
            if (distance_from_touching < 0.0) {
                incremental_change =
                        (anticollisionSpringScalars[layoutPass] *
                                (anticollisionSpringStrength *
                                        (1.0 -
                                                (
                                                        ((node_view_radius + other_node_view_radius) *
                                                                (delta_x * delta_x)
                                                        ) /
                                                                euclidean_distance_cubed
                                                )
                                        )
                                )
                        );
                if (reversed) {
                    if (other_node_partials != null) {
                        other_node_partials.yy -= incremental_change;
                    }
                } else {
                    partials.yy += incremental_change;
                    if (other_node_partials != null) {
                        other_node_partials.yy += incremental_change;
                    }
                }
            }
            incremental_change =
                    (nodeDistanceSpringScalars[layoutPass] *
                            (nodeDistanceSpringStrengths[node_view_index][other_node_view_index] *
                                    ((nodeDistanceSpringRestLengths[node_view_index][other_node_view_index] *
                                            (delta_x * delta_y)
                                    ) /
                                            euclidean_distance_cubed
                                    )
                            )
                    );

            //System.out.println( "Incremental_Change: "+incremental_change );

            if (reversed) {
                if (other_node_partials != null) {
                    other_node_partials.xy -= incremental_change;
                }
            } else {
                partials.xy += incremental_change;
                if (other_node_partials != null) {
                    other_node_partials.xy += incremental_change;
                }
            }
            if (distance_from_touching < 0.0) {
                incremental_change =
                        (anticollisionSpringScalars[layoutPass] *
                                (anticollisionSpringStrength *
                                        (
                                                ((node_view_radius + other_node_view_radius) *
                                                        (delta_x * delta_y)
                                                ) /
                                                        euclidean_distance_cubed
                                        )
                                )
                        );
                if (reversed) {
                    if (other_node_partials != null) {
                        other_node_partials.xy -= incremental_change;
                    }
                } else {
                    partials.xy += incremental_change;
                    if (other_node_partials != null) {
                        other_node_partials.xy += incremental_change;
                    }
                }
            }

            distance_from_rest =
                    (euclidean_distance -
                            nodeDistanceSpringRestLengths[node_view_index][other_node_view_index]
                    );
            incremental_change =
                    (nodeDistanceSpringScalars[layoutPass] *
                            ((nodeDistanceSpringStrengths[node_view_index][other_node_view_index] *
                                    (distance_from_rest * distance_from_rest)
                            ) /
                                    2
                            )
                    );

            //System.out.println( "Distance_From_Rest: "+distance_from_rest+" Incremental_Change: "+incremental_change );

            if (reversed) {
                if (other_node_partials != null) {
                    potential_energy.totalEnergy -= incremental_change;
                }
            } else {
                potential_energy.totalEnergy += incremental_change;
                if (other_node_partials != null) {
                    potential_energy.totalEnergy += incremental_change;
                }
            }
            if (distance_from_touching < 0.0) {
                incremental_change =
                        (anticollisionSpringScalars[layoutPass] *
                                ((anticollisionSpringStrength *
                                        (distance_from_touching * distance_from_touching)
                                ) /
                                        2
                                )
                        );
                if (reversed) {
                    if (other_node_partials != null) {
                        potential_energy.totalEnergy -= incremental_change;
                    }
                } else {
                    potential_energy.totalEnergy += incremental_change;
                    if (other_node_partials != null) {
                        potential_energy.totalEnergy += incremental_change;
                    }
                }
            }
            if (other_node_partials != null) {
                other_node_partials.euclideanDistance =
                        Math.sqrt((other_node_partials.x * other_node_partials.x) +
                                (other_node_partials.y * other_node_partials.y));
                if ((furthest_partials == null) ||
                        (other_node_partials.euclideanDistance >
                                furthest_partials.euclideanDistance)
                        ) {
                    furthest_partials = other_node_partials;
                }
            }

        }

        if (!reversed) {
            partials.euclideanDistance =
                    Math.sqrt((partials.x * partials.x) +
                            (partials.y * partials.y));
        }

        if ((furthest_partials == null) ||
                (partials.euclideanDistance >
                        furthest_partials.euclideanDistance)
                ) {
            furthest_partials = partials;
        }

        //System.out.println( "Furthest_Partials: "+furthest_partials );

        return furthest_partials;
    } // calculatePartials( PartialDerivatives, List, PotentialEnergy, boolean )

    /**
     * Move the node with the given partials and adjust all partials in the given
     * List to reflect that move, and adjust the potential energy too.
     *
     * @return the PartialDerivatives of the furthest node after the move.
     */
    protected PartialDerivatives moveNode(PartialDerivatives partials,
                                          List partials_list,
                                          PotentialEnergy potential_energy) {
        View node_view = partials.getNodeView();

        PartialDerivatives starting_partials = new PartialDerivatives(partials);
        calculatePartials(partials,
                partials_list,
                potential_energy,
                true);
        simpleMoveNode(starting_partials);
        return
                calculatePartials(partials,
                        partials_list,
                        potential_energy,
                        false);
    } // moveNode( PartialDerivatives, List, PotentialEnergy )

    protected void simpleMoveNode(PartialDerivatives partials) {
       /* View node_view = partials.getNodeView();
        double denomenator =
                ((partials.xx * partials.yy) -
                        (partials.xy * partials.xy));
        double delta_x =
                (
                        ((-partials.x * partials.yy) -
                                (-partials.y * partials.xy)) /
                                denomenator
                );
        double delta_y =
                (
                        ((-partials.y * partials.xx) -
                                (-partials.x * partials.xy)) /
                                denomenator
                );

        // REMOVE
        //System.out.println( "moving node \"" + node_view + "\" to ( " + ( node_view.getXPosition() + delta_x ) + ", " + ( node_view.getYPosition() + delta_y ) + " )." );

        // TODO: figure out movement
        //node_view.setXPosition(
        //  node_view.getXPosition() + delta_x
        //);
        //node_view.setYPosition(
        //  node_view.getYPosition() + delta_y
        //);

        Point2D p = node_view.getOffset();
        node_view.setOffset(p.getX() + delta_x, p.getY() + delta_y);*/
    	
    	View node_view = partials.getNodeView();
		double denomenator = partials.xx * partials.yy - partials.xy * partials.xy;
		double delta_x = (-partials.x * partials.yy - -partials.y * partials.xy) / denomenator;
		double delta_y = (-partials.y * partials.xx - -partials.x * partials.xy) / denomenator;
		double x = ((Double)node_view.getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION)).doubleValue();
		double y = ((Double)node_view.getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION)).doubleValue();
		node_view.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, Double.valueOf(x + delta_x));
		node_view.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, Double.valueOf(y + delta_y));

    } // simpleMoveNode( PartialDerivatives )

    protected List createPartialsList() {
        return new ArrayList();
    } // createPartialsList()

    class PartialDerivatives {
        protected View nodeView;
        public double x;
        public double y;
        public double xx;
        public double yy;
        public double xy;
        public double euclideanDistance;

        public PartialDerivatives(View node_view) {
            nodeView = node_view;
        }

        public PartialDerivatives(PartialDerivatives copy_from) {
            nodeView = copy_from.getNodeView();
            copyFrom(copy_from);
        }

        public void reset() {
            x = 0.0;
            y = 0.0;
            xx = 0.0;
            yy = 0.0;
            xy = 0.0;
            euclideanDistance = 0.0;
        } // reset()

        public View getNodeView() {
            return nodeView;
        } // getNodeView()

        public void copyFrom(PartialDerivatives other_partial_derivatives) {
            x = other_partial_derivatives.x;
            y = other_partial_derivatives.y;
            xx = other_partial_derivatives.xx;
            yy = other_partial_derivatives.yy;
            xy = other_partial_derivatives.xy;
            euclideanDistance = other_partial_derivatives.euclideanDistance;
        } // copyFrom( PartialDerivatives )

        public String toString() {
            return "PartialDerivatives( \"" + getNodeView() + "\", x=" + x + ", y=" + y + ", xx=" + xx + ", yy=" + yy + ", xy=" + xy + ", euclideanDistance=" + euclideanDistance + " )";
        } // toString()

    } // inner class PartialDerivatives

    class PotentialEnergy {

        public double totalEnergy = 0.0;

        public void reset() {
            totalEnergy = 0.0;
        } // reset()

    } // class PotentialEnergy

    /**
     * Run the Task.
     */
    public void run() {
        calculate();
    }

    /**
     * Non-blocking call to interrupt the task.
     */
    public void halt() {
        this.interrupted = true;
    }

    /**
     * Sets the Task Monitor.
     *
     * @param taskMonitor TaskMonitor Object.
     */
    public void setTaskMonitor(TaskMonitor taskMonitor) {
        if (this.taskMonitor != null) {
            throw new IllegalStateException("Task Monitor is already set.");
        }
        this.taskMonitor = taskMonitor;
    }

    /**
     * Gets the Task Title.
     *
     * @return human readable task title.
     */
    public String getTitle() {
        return new String("Calculating network layout");
    }

    public void interruptDoLayout() {
        this.interrupted = true;
      }

} // class SpringEmbeddedLayouter
