import tornado.httpserver
import tornado.ioloop
import tornado.web
import sqlite3
import sys

_db = sqlite3.connect('assignment.db')
_cursor = _db.cursor()

class itemRequestHandler(tornado.web.RequestHandler):

	def delete(self):
		_cursor.execute("DROP TABLE IF EXISTS items")
		_cursor.execute("CREATE TABLE items (name VARCHAR(255), price REAL, quantity INT, UNIQUE (name))")
		#INSERT DEFAULT RECORDS?
		_db.commit()
		self.write('OK')
 
	def put(self, ID):
	
		price = self.get_argument("price")
		quantity = self.get_argument("quantity")
		
		if (price != "" and quantity == ""):
			#update record
			record = (float(price), int(ID))
			_cursor.execute("UPDATE items SET price=? WHERE name=?", record)
			db.commit()
			self.write('OK')
		elif (quantity != "" and price == ""):
			#update record
			record = (int(quantity), int(ID))
			_cursor.execute("UPDATE items SET quantity=? WHERE name=?", record)
			db.commit()
			self.write('OK')
		 
	def get(self, ID):
	
		price = self.get_argument("price")
		quantity = self.get_argument("quantity")
		value = self.get_argument("value")
		name = ID
		
		if (price == "true" and quantity == "" and value == ""):
			_cursor.execute("SELECT price FROM items WHERE name=?", ID)
			for row in _cursor:
				itemPrice = row[0]
			self.write(name + " unit price: " + str(itemPrice))
			
		elif (quantity == "true" and price == "" and value == ""):
			_cursor.execute("SELECT quantity FROM items WHERE name=?", ID)
			for row in _cursor:
				itemStock = row[0]		
			self.write(name + " stock level: " + str(itemStock))
		
		elif (value == "true" and quantity == "" and price == ""):
			_cursor.execute("SELECT * FROM items WHERE name=?", ID)
			for row in _cursor:
				itemPrice = row[1]
				itemStock = row[2]
			itemVale = float(itemPrice) * int(itemStock)
			self.write(name + " total stock value: " + str(itemStock))

application = tornado.web.Application([
 (r"/item/item1", itemRequestHandler),
 (r"/item/item2", itemRequestHandler),
 (r"/database", itemRequestHandler),
])

if __name__ == "__main__":
 http_server = tornado.httpserver.HTTPServer(application)
 http_server.listen(43210)
 tornado.ioloop.IOLoop.instance().start()
