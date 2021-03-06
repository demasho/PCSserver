import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import Monitoringandcontrol.SendMail;
public class Monitoring
{	
	private final ScheduledExecutorService Subscripers= Executors.newSingleThreadScheduledExecutor();
	private final ScheduledExecutorService Orders= Executors.newSingleThreadScheduledExecutor();
	private final ScheduledExecutorService Complaints= Executors.newSingleThreadScheduledExecutor();
	public static DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
	/*********************************************************************************************/
	public void StartMonitoringSubscripers() 
	{
		final ScheduledFuture<?> taskHandle = Subscripers.scheduleAtFixedRate(new Runnable()
		{
			public void run()
			{
				JSONArray subscriptions=GetAllAlmostExpiredSubs();
				if(subscriptions.length()!=0)
				{ 
					for(int i = 0; i < subscriptions.length(); i ++)
					{
						try
						{
							SendMail.sendSubscriptionRenewEmail(subscriptions.getJSONObject(i).getString("SubscribeID"),subscriptions.getJSONObject(i).getString("start"),subscriptions.getJSONObject(i).getString("email") ,subscriptions.getJSONObject(i).getBoolean("IsB"));
						} 
						catch (JSONException e)
						{
							// TODO Auto-generated catch block
							
						}
					}
				}
			}
		}, 0 , 1 , java.util.concurrent.TimeUnit.DAYS);
	}
	/*********************************************************************************************/
	public void StartMonitoringEnterTimeForOrders() 
	{
		final ScheduledFuture<?> taskHandlestartorders = Orders.scheduleAtFixedRate(new Runnable()
		{
			public void run() 
			{
				JSONArray orders=GetAllLateToPark();
				if(orders.length()!=0)
				{ 
					for(int i = 0; i < orders.length(); i ++)
					{
						try 
						{
							
							SendMail.sendLateAlertMessage(orders.getJSONObject(i).getString("orderID"),orders.getJSONObject(i).getString("start"),orders.getJSONObject(i).getString("parkingID") ,orders.getJSONObject(i).getString("email"));
						}
						catch (JSONException e)
						{
							// TODO Auto-generated catch block
							
						}
					}
				}
			}
		}, 0 , 10 , java.util.concurrent.TimeUnit.MINUTES);
	}
	/*********************************************************************************************/
	public void StartMonitoringEndTimeForOrders()
	{
		final ScheduledFuture<?> taskHandleendorders = Orders.scheduleAtFixedRate(new Runnable()
		{
			public void run()
			{
				JSONArray orders=GetAllExceededParkingTime();
				if(orders.length()!=0)
				{ 
					for(int i = 0; i < orders.length(); i ++)
					{
						try
						{
							
							SendMail.sendExcessionEmail(orders.getJSONObject(i).getString("orderID"),orders.getJSONObject(i).getString("start"),orders.getJSONObject(i).getString("parkingID") ,orders.getJSONObject(i).getString("email"));
						} 
						catch (JSONException e) 
						{
							// TODO Auto-generated catch block
							
						}
					}
				}
			}
		}, 0 ,59 , java.util.concurrent.TimeUnit.MINUTES);
	}
	/*********************************************************************************************/
	public void StartMonitoringComplaints() 
	{
		final ScheduledFuture<?> taskHandleComplaints = Complaints.scheduleAtFixedRate(new Runnable()
		{
			public void run() 
			{
				JSONArray Complaints=GetAllComplaint();
				if(Complaints.length()!=0)
				{ 
					try 
					{
						for(int i = 0; i < Complaints.length(); i ++)
						{
							JSONArray Workers=GetAllWorkers(Complaints.getJSONObject(i).getString("ParkingID"));
							for(int j=0;j<Workers.length();j++) {
								
								SendMail.sendAlertForComplaintEmail(Complaints.getJSONObject(i).getString("ComplaintID"), Complaints.getJSONObject(i).getString("AddDate"), Workers.getJSONObject(j).getString("WorkerID"), Workers.getJSONObject(j).getString("email") , Complaints.getJSONObject(i).getString("ParkingID"));
							}
						}
					}

					catch (JSONException e)
					{
						// TODO Auto-generated catch block
						
					}
				}
			}
		}, 0 ,2 , java.util.concurrent.TimeUnit.HOURS);
	}
	/*********************************************************************************************/
	private JSONArray GetAllComplaint(){
		JSONArray AllComplaint=null;
		try {
			ResultSet rs=ConnectionToDataBaseSQL.GetAllComplaints();
			AllComplaint = new JSONArray();
			while(rs.next())
			{
				//`customerID`, `complaintID`, `AddDate`, `description`, `parkingID` 
				AllComplaint.put(new JSONObject()
						.put("ComplaintID",rs.getString(2))
						.put("AddDate",rs.getString(3))
						.put("Des", rs.getString(4))
						.put("ParkingID", rs.getString(5))
						);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			
		}
		return AllComplaint;
	}
	/*********************************************************************************************/
	private JSONArray GetAllWorkers(String ParkingID) {		
		JSONArray AllWorkersInParking = new JSONArray();
		try {
			ResultSet rs=ConnectionToDataBaseSQL.GetAllServiceWorkersInParking(ParkingID);
			while(rs.next())
			{
				//`WorkerID`,email
				AllWorkersInParking.put(new JSONObject()
						.put("WorkerID",rs.getString(1))
						.put("email",rs.getString(2))
						);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			
		}
		return AllWorkersInParking;
	}
	/*********************************************************************************************/
	private JSONArray GetAllAlmostExpiredSubs()
	{
		JSONArray subscriptions = new JSONArray();
		Date now =new  Date();
		ResultSet rs = ConnectionToDataBaseSQL.GetAllSubscriper();
		java.util.Date DeadLine;
		try {
			while(rs.next())
			{
				
				DeadLine =  format.parse(rs.getString(2));
				long diff = DeadLine.getTime() - now.getTime();
				int days=(int) (diff / (1000*60*60*24));
				
				if(days<=7)
				{
					subscriptions.put(new JSONObject()
							.put("SubscribeID", rs.getInt(1))
							.put("start",rs.getString(2))
							.put("email",rs.getString(3))
							.put("IsB", Boolean.parseBoolean(rs.getString(4)))
							);
				}
			}
		}
		catch(Exception e)
		{
			
		}
		return subscriptions;
	}
	/*********************************************************************************************/
	private JSONArray GetAllLateToPark()
	{
		JSONArray OneTimeOrders = new JSONArray();
		Date now =new  Date();
		ResultSet rs = ConnectionToDataBaseSQL.GetAllOneTimeOrders();
		java.util.Date StartLine;
		try 
		{
			while(rs.next()){
				StartLine =  format.parse(rs.getString(2));
				boolean res=now.before(StartLine);
				boolean isInside = ParkingNetwork.getParking(rs.getString(3)).isInsideParking(rs.getString(5),Integer.toString(rs.getInt(1)));
				if(res == false && isInside == false && rs.getBoolean(6)==false)
				{					
					ConnectionToDataBaseSQL.putisLateToParkFlag(rs.getInt(1));
					ConnectionToDataBaseSQL.PutFine(rs.getInt(1),1.2);
					OneTimeOrders.put(new JSONObject()
							.put("orderID", rs.getInt(1))
							.put("start",rs.getString(2))
							.put("parkingID",rs.getString(3))
							.put("email", rs.getString(4))
							);
				}
			}
		}
		catch(Exception e)
		{
			
		}
		return OneTimeOrders;
	}
	/*********************************************************************************************/
	private JSONArray GetAllExceededParkingTime()
	{
		JSONArray OneTimeOrders = new JSONArray();
		Date now =new  Date();
		ResultSet rs = ConnectionToDataBaseSQL.GetAllOrders();
		java.util.Date DeadLine;
		try 
		{
			while(rs.next())
			{
				DeadLine =  format.parse(rs.getString(2));
				boolean res=now.after(DeadLine);
				boolean isInside = ParkingNetwork.getParking(rs.getString(3)).isInsideParking(rs.getString(5),Integer.toString(rs.getInt(1)));
				if(res == true && isInside == true)
				{	
					ConnectionToDataBaseSQL.PutFine(rs.getInt(1),1.3);
					OneTimeOrders.put(new JSONObject()
							.put("orderID", rs.getInt(1))
							.put("start",rs.getString(2))
							.put("parkingID",rs.getString(3))
							.put("email", rs.getString(4))
							);
				}
			}
		}
		catch(Exception e)
		{
			
		}
		return OneTimeOrders;
	}
	/*********************************************************************************************/
}