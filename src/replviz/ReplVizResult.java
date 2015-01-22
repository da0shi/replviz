package replviz;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.Modifier;
import java.lang.StringBuilder;
import java.util.ArrayList;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxICell;
import com.mxgraph.model.mxIGraphModel;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;

import static replviz.Utils.extractType;
import static replviz.Utils.getType;
import static replviz.Utils.isNotPrimitive;

public class ReplVizResult
{
	private final String key;
	private final Object value;
	private final Type type;

	private mxCell refCell;
	private mxCell entityCell;

	public ReplVizResult (String key, Object value, Type type)
	{
		this.key = key;
		this.value = value;
		this.type = type;
	}

	public String key ()
	{
		return key;
	}
	public Object value ()
	{
		return value;
	}
	public Type type ()
	{
		if (value != null) {
			return extractType(type);
		}
		return extractType(Object.class);
	}
	public String strType ()
	{
		return getType(type);
	}
	public String valueRef ()
	{
		if (value == null) return "Null";
			if (isNotPrimitive(type)) {
			return getType(extractType(value.getClass())) +"@"+ Integer.toHexString(value.hashCode());
		}
		return value.toString();
	}

	public void insertVar (mxGraph graph, mxCell refbox)
	{
		Object parent = graph.getDefaultParent();
		graph.getModel().beginUpdate();
		try {
			int y = 0;
			int childCount = refbox.getChildCount();
			if (childCount > 0) {
				mxICell lastChild = refbox.getChildAt(childCount - 1);
				mxGeometry geo = lastChild.getGeometry();
				y = (int)(geo.getY() + geo.getHeight());
			}
			refCell  = (mxCell) graph.insertVertex(refbox, null,
					strType() +"  "+  key +" = "+ valueRef(),
					0, y, ReplViz.VARIABLE_WIDTH, ReplViz.VARIABLE_HEIGHT);
			if (y == 0) y = 40;
			if (isNotPrimitive(type)) {
				entityCell = insertEntity(graph);
				graph.insertEdge(parent, null, null, refCell, entityCell);
			}
		}
		catch (NullPointerException e) {
			e.printStackTrace();
		}
		finally {
			graph.getModel().endUpdate();
		}
	}

	private mxCell insertEntity (mxGraph graph)
	{
		Object parent = graph.getDefaultParent();
		mxIGraphModel model = graph.getModel();
		mxCell frame = null;
		model.beginUpdate();
		try {
			frame = (mxCell) graph.insertVertex(
					parent, null, valueRef(), ReplViz.VARIABLE_LIST_WIDTH + 40, 0,
					ReplViz.VARIABLE_LIST_WIDTH, ReplViz.VARIABLE_HEIGHT,
					"shape=swimlane;foldable=0;fillColor=#999;fontColor=#000");

			// if toString has been overridden
			if (! Object.class.equals(value.getClass().getMethod("toString").getDeclaringClass())) {
				mxCell field  = (mxCell) graph.insertVertex(frame, null,
						value.toString().trim(),
						0, 0, ReplViz.VARIABLE_WIDTH, ReplViz.VARIABLE_HEIGHT);
			}
			else {
				Field[] fields = value.getClass().getFields();
				int y = 0;
				for (Field f : fields) {
					int childCount = frame.getChildCount();
					if (childCount > 0) {
						mxICell lastChild = frame.getChildAt(childCount - 1);
						mxGeometry geo = lastChild.getGeometry();
						y = (int)(geo.getY() + geo.getHeight());
					}

					mxCell field  = (mxCell) graph.insertVertex(frame, null,
							getType(f.getType()) +"  "+  f.getName() +" = "+ f.get(value),
							0, y, ReplViz.VARIABLE_WIDTH, ReplViz.VARIABLE_HEIGHT);
				}
			}
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
		finally {
			graph.getModel().endUpdate();
		}


		return frame;
	}

	public void removeVar (mxGraph graph)
	{
		mxIGraphModel model = graph.getModel();
		model.beginUpdate();
		try {
			Object[] edges = graph.getEdgesBetween(refCell, entityCell);
			for (Object edge: edges) {
				model.remove(edge);
			}
			model.remove(refCell);
			model.remove(entityCell);
		}
		finally {
			model.endUpdate();
		}
	}

}
